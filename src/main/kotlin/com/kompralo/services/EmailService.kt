package com.kompralo.services

import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.model.Message
import jakarta.activation.DataHandler
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import jakarta.mail.util.ByteArrayDataSource
import org.apache.commons.codec.binary.Base64
import com.kompralo.port.EmailPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale
import java.util.Properties

@Service
class EmailService(
    private val gmail: Gmail
) : EmailPort {
    private val logger = LoggerFactory.getLogger(EmailService::class.java)
    private val session: Session = Session.getDefaultInstance(Properties())
    private val copLocale = Locale("es", "CO")

    @Value("\${gmail.api.from-address}")
    private lateinit var fromAddress: String

    @Value("\${gmail.api.from-name}")
    private lateinit var fromName: String

    @Value("\${app.frontend-url}")
    private lateinit var frontendUrl: String

    private fun formatCOP(amount: BigDecimal): String =
        NumberFormat.getCurrencyInstance(copLocale).apply { maximumFractionDigits = 0 }.format(amount)

    private fun escapeHtml(text: String): String =
        text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private fun sendGmailMessage(mimeMessage: MimeMessage): Boolean {
        return try {
            val buffer = ByteArrayOutputStream()
            mimeMessage.writeTo(buffer)
            val rawSize = buffer.size()
            logger.info("Email MIME size: $rawSize bytes")
            val encodedEmail = Base64.encodeBase64URLSafeString(buffer.toByteArray())

            val result = gmail.users().messages().send("me", Message().apply { raw = encodedEmail }).execute()

            val to = mimeMessage.getRecipients(jakarta.mail.Message.RecipientType.TO)
                ?.joinToString { it.toString() } ?: "desconocido"
            logger.info("Email enviado correctamente a: $to (Gmail ID: ${result.id})")
            true
        } catch (e: Exception) {
            val to = try {
                mimeMessage.getRecipients(jakarta.mail.Message.RecipientType.TO)
                    ?.joinToString { it.toString() } ?: "desconocido"
            } catch (_: Exception) { "desconocido" }
            logger.error("Error al enviar email a $to: ${e.javaClass.simpleName} - ${e.message}", e)
            false
        }
    }

    private fun buildMimeMessage(to: String, subject: String, htmlContent: String, attachmentName: String? = null, attachmentBytes: ByteArray? = null): MimeMessage {
        return MimeMessage(session).apply {
            setFrom(InternetAddress(fromAddress, fromName))
            setRecipient(jakarta.mail.Message.RecipientType.TO, InternetAddress(to))
            setSubject(subject, "UTF-8")

            if (attachmentName != null && attachmentBytes != null) {
                val multipart = MimeMultipart("mixed")
                multipart.addBodyPart(MimeBodyPart().apply {
                    setContent(htmlContent, "text/html; charset=UTF-8")
                })
                multipart.addBodyPart(MimeBodyPart().apply {
                    dataHandler = DataHandler(ByteArrayDataSource(attachmentBytes, "application/pdf"))
                    fileName = attachmentName
                })
                setContent(multipart)
            } else {
                setContent(htmlContent, "text/html; charset=UTF-8")
            }
        }
    }

    override fun sendHtmlEmailWithAttachment(
        to: String,
        subject: String,
        htmlContent: String,
        attachmentName: String?,
        attachmentBytes: ByteArray?
    ): Boolean = sendGmailMessage(buildMimeMessage(to, subject, htmlContent, attachmentName, attachmentBytes))

    override fun sendOrderConfirmationToBuyer(
        buyerEmail: String,
        buyerName: String,
        orderNumber: String,
        total: BigDecimal,
        itemCount: Int,
        sellerName: String,
        pdfReceipt: ByteArray
    ): Boolean {
        val safeName = escapeHtml(buyerName)
        val safeSeller = escapeHtml(sellerName)
        val safeOrder = escapeHtml(orderNumber)

        val html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background:#f3f4f6;font-family:Arial,Helvetica,sans-serif;">
              <table width="100%" cellpadding="0" cellspacing="0" style="background:#f3f4f6;padding:40px 0;">
                <tr><td align="center">
                  <table width="600" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 6px rgba(0,0,0,0.07);">
                    <tr>
                      <td style="background:linear-gradient(135deg,#059669,#10b981);padding:32px;text-align:center;">
                        <h1 style="color:#ffffff;margin:0;font-size:28px;letter-spacing:1px;">KOMPRALO</h1>
                        <p style="color:#d1fae5;margin:8px 0 0;font-size:14px;">Comprobante de Compra</p>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:32px;">
                        <h2 style="color:#111827;margin:0 0 8px;font-size:20px;">Hola $safeName,</h2>
                        <p style="color:#6b7280;margin:0 0 24px;font-size:14px;line-height:1.6;">
                          Tu pedido ha sido creado exitosamente. Adjuntamos el comprobante en PDF.
                        </p>
                        <table width="100%" cellpadding="0" cellspacing="0" style="background:#f0fdf4;border-radius:12px;padding:20px;margin-bottom:24px;">
                          <tr><td style="padding:20px;">
                            <table width="100%" cellpadding="0" cellspacing="0">
                              <tr>
                                <td style="color:#6b7280;font-size:12px;padding-bottom:8px;">Pedido</td>
                                <td style="color:#111827;font-size:14px;font-weight:bold;text-align:right;padding-bottom:8px;">$safeOrder</td>
                              </tr>
                              <tr>
                                <td style="color:#6b7280;font-size:12px;padding-bottom:8px;">Tienda</td>
                                <td style="color:#111827;font-size:14px;text-align:right;padding-bottom:8px;">$safeSeller</td>
                              </tr>
                              <tr>
                                <td style="color:#6b7280;font-size:12px;padding-bottom:8px;">Productos</td>
                                <td style="color:#111827;font-size:14px;text-align:right;padding-bottom:8px;">$itemCount item(s)</td>
                              </tr>
                              <tr>
                                <td colspan="2" style="border-top:1px solid #d1fae5;padding-top:12px;"></td>
                              </tr>
                              <tr>
                                <td style="color:#059669;font-size:16px;font-weight:bold;">Total</td>
                                <td style="color:#059669;font-size:18px;font-weight:bold;text-align:right;">${formatCOP(total)}</td>
                              </tr>
                            </table>
                          </td></tr>
                        </table>
                        <table width="100%" cellpadding="0" cellspacing="0">
                          <tr><td align="center">
                            <a href="$frontendUrl/mis-pedidos" style="display:inline-block;background:#059669;color:#ffffff;text-decoration:none;padding:14px 32px;border-radius:8px;font-size:14px;font-weight:bold;">
                              Ver mis pedidos
                            </a>
                          </td></tr>
                        </table>
                      </td>
                    </tr>
                    <tr>
                      <td style="background:#f9fafb;padding:20px;text-align:center;border-top:1px solid #e5e7eb;">
                        <p style="color:#9ca3af;margin:0;font-size:12px;">Kompralo Marketplace - Gracias por tu compra</p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
        """.trimIndent()

        return sendHtmlEmailWithAttachment(
            to = buyerEmail,
            subject = "Comprobante de tu pedido $safeOrder - Kompralo",
            htmlContent = html,
            attachmentName = "Comprobante_${orderNumber}.pdf",
            attachmentBytes = pdfReceipt
        )
    }

    override fun sendNewOrderNotificationToStore(
        sellerEmail: String,
        sellerName: String,
        orderNumber: String,
        buyerName: String,
        total: BigDecimal,
        itemCount: Int
    ): Boolean {
        val safeSeller = escapeHtml(sellerName)
        val safeBuyer = escapeHtml(buyerName)
        val safeOrder = escapeHtml(orderNumber)

        val html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background:#f3f4f6;font-family:Arial,Helvetica,sans-serif;">
              <table width="100%" cellpadding="0" cellspacing="0" style="background:#f3f4f6;padding:40px 0;">
                <tr><td align="center">
                  <table width="600" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 6px rgba(0,0,0,0.07);">
                    <tr>
                      <td style="background:linear-gradient(135deg,#059669,#10b981);padding:32px;text-align:center;">
                        <h1 style="color:#ffffff;margin:0;font-size:28px;letter-spacing:1px;">KOMPRALO</h1>
                        <p style="color:#d1fae5;margin:8px 0 0;font-size:14px;">Nuevo Pedido Recibido</p>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:32px;">
                        <h2 style="color:#111827;margin:0 0 8px;font-size:20px;">Hola $safeSeller,</h2>
                        <p style="color:#6b7280;margin:0 0 24px;font-size:14px;line-height:1.6;">
                          Tienes un nuevo pedido. Revisa los detalles y preparalo para envio.
                        </p>
                        <table width="100%" cellpadding="0" cellspacing="0" style="background:#fffbeb;border-radius:12px;margin-bottom:24px;">
                          <tr><td style="padding:20px;">
                            <table width="100%" cellpadding="0" cellspacing="0">
                              <tr>
                                <td style="color:#6b7280;font-size:12px;padding-bottom:8px;">Pedido</td>
                                <td style="color:#111827;font-size:14px;font-weight:bold;text-align:right;padding-bottom:8px;">$safeOrder</td>
                              </tr>
                              <tr>
                                <td style="color:#6b7280;font-size:12px;padding-bottom:8px;">Comprador</td>
                                <td style="color:#111827;font-size:14px;text-align:right;padding-bottom:8px;">$safeBuyer</td>
                              </tr>
                              <tr>
                                <td style="color:#6b7280;font-size:12px;padding-bottom:8px;">Productos</td>
                                <td style="color:#111827;font-size:14px;text-align:right;padding-bottom:8px;">$itemCount item(s)</td>
                              </tr>
                              <tr>
                                <td colspan="2" style="border-top:1px solid #fde68a;padding-top:12px;"></td>
                              </tr>
                              <tr>
                                <td style="color:#d97706;font-size:16px;font-weight:bold;">Total</td>
                                <td style="color:#d97706;font-size:18px;font-weight:bold;text-align:right;">${formatCOP(total)}</td>
                              </tr>
                            </table>
                          </td></tr>
                        </table>
                        <table width="100%" cellpadding="0" cellspacing="0">
                          <tr><td align="center">
                            <a href="$frontendUrl/admin/orders" style="display:inline-block;background:#059669;color:#ffffff;text-decoration:none;padding:14px 32px;border-radius:8px;font-size:14px;font-weight:bold;">
                              Ver pedidos
                            </a>
                          </td></tr>
                        </table>
                      </td>
                    </tr>
                    <tr>
                      <td style="background:#f9fafb;padding:20px;text-align:center;border-top:1px solid #e5e7eb;">
                        <p style="color:#9ca3af;margin:0;font-size:12px;">Kompralo Marketplace - Panel de Vendedor</p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
        """.trimIndent()

        return sendHtmlEmailWithAttachment(
            to = sellerEmail,
            subject = "Nuevo pedido $safeOrder - Kompralo",
            htmlContent = html
        )
    }

    override fun sendPasswordResetEmail(to: String, userName: String, resetToken: String, resetUrl: String): Boolean {
        val safeName = escapeHtml(userName)
        val safeToken = escapeHtml(resetToken)
        val safeUrl = escapeHtml(resetUrl)

        val html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background:#f3f4f6;font-family:Arial,Helvetica,sans-serif;">
              <table width="100%" cellpadding="0" cellspacing="0" style="background:#f3f4f6;padding:40px 0;">
                <tr><td align="center">
                  <table width="600" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 6px rgba(0,0,0,0.07);">
                    <tr>
                      <td style="background:linear-gradient(135deg,#059669,#10b981);padding:32px;text-align:center;">
                        <h1 style="color:#ffffff;margin:0;font-size:28px;letter-spacing:1px;">KOMPRALO</h1>
                        <p style="color:#d1fae5;margin:8px 0 0;font-size:14px;">Recuperacion de Contrasena</p>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:32px;">
                        <h2 style="color:#111827;margin:0 0 8px;font-size:20px;">Hola $safeName,</h2>
                        <p style="color:#6b7280;margin:0 0 24px;font-size:14px;line-height:1.6;">
                          Hemos recibido una solicitud para restablecer tu contrasena en Kompralo.
                        </p>
                        <table width="100%" cellpadding="0" cellspacing="0">
                          <tr><td align="center" style="padding-bottom:24px;">
                            <a href="$safeUrl?token=$safeToken" style="display:inline-block;background:#059669;color:#ffffff;text-decoration:none;padding:14px 32px;border-radius:8px;font-size:14px;font-weight:bold;">
                              Restablecer contrasena
                            </a>
                          </td></tr>
                        </table>
                        <p style="color:#9ca3af;margin:0;font-size:12px;line-height:1.6;">
                          Este enlace expirara en 24 horas. Si no solicitaste restablecer tu contrasena, puedes ignorar este correo de forma segura.
                        </p>
                      </td>
                    </tr>
                    <tr>
                      <td style="background:#f9fafb;padding:20px;text-align:center;border-top:1px solid #e5e7eb;">
                        <p style="color:#9ca3af;margin:0;font-size:12px;">Equipo de Kompralo</p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
        """.trimIndent()

        return sendHtmlEmailWithAttachment(to = to, subject = "Recuperacion de contrasena - Kompralo", htmlContent = html)
    }

    override fun sendSellerWelcomeEmail(to: String, businessName: String): Boolean {
        val safeName = escapeHtml(businessName)

        val html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background:#f3f4f6;font-family:Arial,Helvetica,sans-serif;">
              <table width="100%" cellpadding="0" cellspacing="0" style="background:#f3f4f6;padding:40px 0;">
                <tr><td align="center">
                  <table width="600" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 6px rgba(0,0,0,0.07);">
                    <tr>
                      <td style="background:linear-gradient(135deg,#059669,#10b981);padding:32px;text-align:center;">
                        <h1 style="color:#ffffff;margin:0;font-size:28px;letter-spacing:1px;">KOMPRALO</h1>
                        <p style="color:#d1fae5;margin:8px 0 0;font-size:14px;">Bienvenido Vendedor</p>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:32px;">
                        <h2 style="color:#111827;margin:0 0 8px;font-size:20px;">Hola $safeName,</h2>
                        <p style="color:#6b7280;margin:0 0 16px;font-size:14px;line-height:1.6;">
                          Bienvenido a Kompralo! Tu cuenta de vendedor ha sido creada exitosamente.
                        </p>
                        <p style="color:#6b7280;margin:0 0 24px;font-size:14px;line-height:1.6;">
                          Estamos revisando tu informacion y te notificaremos cuando tu cuenta sea verificada. Mientras tanto puedes:
                        </p>
                        <ul style="color:#374151;font-size:14px;line-height:2;">
                          <li>Completar tu perfil de negocio</li>
                          <li>Preparar tus productos para publicar</li>
                          <li>Familiarizarte con nuestra plataforma</li>
                        </ul>
                      </td>
                    </tr>
                    <tr>
                      <td style="background:#f9fafb;padding:20px;text-align:center;border-top:1px solid #e5e7eb;">
                        <p style="color:#9ca3af;margin:0;font-size:12px;">Equipo de Kompralo</p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
        """.trimIndent()

        return sendHtmlEmailWithAttachment(to = to, subject = "Bienvenido a Kompralo!", htmlContent = html)
    }

    override fun sendProblemReport(
        userEmail: String,
        userName: String,
        tipo: String,
        seccion: String,
        descripcion: String
    ): Boolean {
        val safeName = escapeHtml(userName)
        val safeEmail = escapeHtml(userEmail)
        val safeTipo = escapeHtml(tipo)
        val safeSeccion = escapeHtml(seccion)
        val safeDescripcion = escapeHtml(descripcion).replace("\n", "<br>")

        val html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background:#f3f4f6;font-family:Arial,Helvetica,sans-serif;">
              <table width="100%" cellpadding="0" cellspacing="0" style="background:#f3f4f6;padding:40px 0;">
                <tr><td align="center">
                  <table width="600" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 6px rgba(0,0,0,0.07);">
                    <tr>
                      <td style="background:linear-gradient(135deg,#d97706,#f59e0b);padding:32px;text-align:center;">
                        <h1 style="color:#ffffff;margin:0;font-size:28px;letter-spacing:1px;">KOMPRALO</h1>
                        <p style="color:#fef3c7;margin:8px 0 0;font-size:14px;">Reporte de Problema</p>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:32px;">
                        <h2 style="color:#111827;margin:0 0 16px;font-size:20px;">Nuevo reporte recibido</h2>
                        <table width="100%" cellpadding="0" cellspacing="0" style="background:#fffbeb;border-radius:12px;margin-bottom:24px;">
                          <tr><td style="padding:20px;">
                            <table width="100%" cellpadding="0" cellspacing="0">
                              <tr>
                                <td style="color:#6b7280;font-size:12px;padding-bottom:8px;">Tipo de problema</td>
                                <td style="color:#111827;font-size:14px;font-weight:bold;text-align:right;padding-bottom:8px;">$safeTipo</td>
                              </tr>
                              <tr>
                                <td style="color:#6b7280;font-size:12px;padding-bottom:8px;">Seccion afectada</td>
                                <td style="color:#111827;font-size:14px;text-align:right;padding-bottom:8px;">$safeSeccion</td>
                              </tr>
                              <tr>
                                <td style="color:#6b7280;font-size:12px;padding-bottom:8px;">Reportado por</td>
                                <td style="color:#111827;font-size:14px;text-align:right;padding-bottom:8px;">$safeName ($safeEmail)</td>
                              </tr>
                            </table>
                          </td></tr>
                        </table>
                        <div style="background:#f9fafb;border-radius:12px;padding:20px;margin-bottom:16px;">
                          <p style="color:#6b7280;font-size:12px;margin:0 0 8px;font-weight:bold;">Descripcion</p>
                          <p style="color:#374151;font-size:14px;line-height:1.6;margin:0;">$safeDescripcion</p>
                        </div>
                      </td>
                    </tr>
                    <tr>
                      <td style="background:#f9fafb;padding:20px;text-align:center;border-top:1px solid #e5e7eb;">
                        <p style="color:#9ca3af;margin:0;font-size:12px;">Kompralo Marketplace - Reporte Automatico</p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
        """.trimIndent()

        return sendHtmlEmailWithAttachment(
            to = fromAddress,
            subject = "[$safeTipo] Reporte de Problema - $safeName",
            htmlContent = html
        )
    }

    override fun sendSellerVerifiedEmail(to: String, businessName: String): Boolean {
        val safeName = escapeHtml(businessName)

        val html = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background:#f3f4f6;font-family:Arial,Helvetica,sans-serif;">
              <table width="100%" cellpadding="0" cellspacing="0" style="background:#f3f4f6;padding:40px 0;">
                <tr><td align="center">
                  <table width="600" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 6px rgba(0,0,0,0.07);">
                    <tr>
                      <td style="background:linear-gradient(135deg,#059669,#10b981);padding:32px;text-align:center;">
                        <h1 style="color:#ffffff;margin:0;font-size:28px;letter-spacing:1px;">KOMPRALO</h1>
                        <p style="color:#d1fae5;margin:8px 0 0;font-size:14px;">Cuenta Verificada</p>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:32px;">
                        <h2 style="color:#111827;margin:0 0 8px;font-size:20px;">Hola $safeName,</h2>
                        <p style="color:#6b7280;margin:0 0 16px;font-size:14px;line-height:1.6;">
                          Excelentes noticias! Tu cuenta de vendedor ha sido verificada exitosamente.
                        </p>
                        <p style="color:#6b7280;margin:0 0 24px;font-size:14px;line-height:1.6;">
                          Ya puedes empezar a publicar productos y vender en Kompralo. Te deseamos mucho exito en tus ventas!
                        </p>
                        <table width="100%" cellpadding="0" cellspacing="0">
                          <tr><td align="center">
                            <a href="$frontendUrl/admin/products" style="display:inline-block;background:#059669;color:#ffffff;text-decoration:none;padding:14px 32px;border-radius:8px;font-size:14px;font-weight:bold;">
                              Publicar productos
                            </a>
                          </td></tr>
                        </table>
                      </td>
                    </tr>
                    <tr>
                      <td style="background:#f9fafb;padding:20px;text-align:center;border-top:1px solid #e5e7eb;">
                        <p style="color:#9ca3af;margin:0;font-size:12px;">Equipo de Kompralo</p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
        """.trimIndent()

        return sendHtmlEmailWithAttachment(to = to, subject = "Tu cuenta de vendedor ha sido verificada!", htmlContent = html)
    }
}
