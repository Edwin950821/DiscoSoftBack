package com.kompralo.services

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

/**
 * Servicio para envío de emails
 *
 * Proporciona funcionalidad básica para enviar correos electrónicos
 * usando Spring Mail con Gmail SMTP
 */
@Service
class EmailService(
    private val mailSender: JavaMailSender
) {

    private val logger = LoggerFactory.getLogger(EmailService::class.java)

    @Value("\${mail.from-address}")
    private lateinit var fromAddress: String

    @Value("\${mail.from-name}")
    private lateinit var fromName: String

    /**
     * Envía un email simple de texto plano
     *
     * @param to Dirección de email del destinatario
     * @param subject Asunto del email
     * @param text Contenido del email en texto plano
     * @return true si el email se envió correctamente, false en caso de error
     */
    fun sendEmail(to: String, subject: String, text: String): Boolean {
        return try {
            val message = SimpleMailMessage()
            message.from = "$fromName <$fromAddress>"
            message.setTo(to)
            message.subject = subject
            message.text = text

            mailSender.send(message)
            logger.info("Email enviado correctamente a: $to")
            true
        } catch (e: Exception) {
            logger.error("Error al enviar email a $to: ${e.message}", e)
            false
        }
    }

    /**
     * Envía un email de recuperación de contraseña
     *
     * @param to Email del usuario
     * @param userName Nombre del usuario
     * @param resetToken Token de recuperación
     * @param resetUrl URL completa para resetear (frontend)
     */
    fun sendPasswordResetEmail(to: String, userName: String, resetToken: String, resetUrl: String): Boolean {
        val subject = "Recuperación de contraseña - Kompralo"
        val text = """
            Hola $userName,

            Hemos recibido una solicitud para restablecer tu contraseña en Kompralo.

            Para restablecer tu contraseña, haz clic en el siguiente enlace:
            $resetUrl?token=$resetToken

            Este enlace expirará en 24 horas.

            Si no solicitaste restablecer tu contraseña, puedes ignorar este correo de forma segura.

            Saludos,
            Equipo de Kompralo
        """.trimIndent()

        return sendEmail(to, subject, text)
    }

    /**
     * Envía un email de bienvenida a nuevos vendedores
     *
     * @param to Email del vendedor
     * @param businessName Nombre del negocio
     */
    fun sendSellerWelcomeEmail(to: String, businessName: String): Boolean {
        val subject = "¡Bienvenido a Kompralo!"
        val text = """
            Hola $businessName,

            ¡Bienvenido a Kompralo!

            Tu cuenta de vendedor ha sido creada exitosamente. Estamos revisando tu información
            y te notificaremos cuando tu cuenta sea verificada.

            Mientras tanto, puedes:
            - Completar tu perfil de negocio
            - Preparar tus productos para publicar
            - Familiarizarte con nuestra plataforma

            Si tienes alguna pregunta, no dudes en contactarnos.

            Saludos,
            Equipo de Kompralo
        """.trimIndent()

        return sendEmail(to, subject, text)
    }

    /**
     * Envía un email de verificación de vendedor aprobada
     *
     * @param to Email del vendedor
     * @param businessName Nombre del negocio
     */
    fun sendSellerVerifiedEmail(to: String, businessName: String): Boolean {
        val subject = "¡Tu cuenta de vendedor ha sido verificada!"
        val text = """
            Hola $businessName,

            ¡Excelentes noticias!

            Tu cuenta de vendedor ha sido verificada exitosamente. Ya puedes empezar a
            publicar productos y vender en Kompralo.

            ¡Te deseamos mucho éxito en tus ventas!

            Saludos,
            Equipo de Kompralo
        """.trimIndent()

        return sendEmail(to, subject, text)
    }
}
