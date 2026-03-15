package com.kompralo.services

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.oned.Code128Writer
import com.google.zxing.qrcode.QRCodeWriter
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.layout.properties.VerticalAlignment
import com.kompralo.config.ShippingConfiguration
import com.kompralo.model.Order
import com.kompralo.model.PaymentMethod
import com.kompralo.port.PdfPort
import com.kompralo.repository.OrderRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.security.MessageDigest
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.imageio.ImageIO

@Service
class PdfService(
    private val orderRepository: OrderRepository,
    private val shippingConfig: ShippingConfiguration
) : PdfPort {

    private val black = DeviceRgb(0, 0, 0)
    private val darkText = DeviceRgb(30, 30, 30)
    private val grayText = DeviceRgb(100, 100, 100)
    private val white = DeviceRgb(255, 255, 255)

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm:ss a")
    private val fullDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    private val receiptWidth = PageSize(255f, 14400f)

    private val logoBytes: ByteArray? by lazy {
        javaClass.classLoader.getResourceAsStream("Kompralo.png")?.readBytes()
    }

    @Transactional(readOnly = true)
    override fun generateReceiptById(orderId: Long): ByteArray? {
        val order = orderRepository.findByIdWithDetails(orderId) ?: return null
        return generateReceipt(order)
    }

    override fun generateReceipt(order: Order): ByteArray {
        val topM = 12f
        val bottomM = 12f
        val leftM = 10f
        val rightM = 10f

        val measureBaos = ByteArrayOutputStream()
        val measurePdf = PdfDocument(PdfWriter(measureBaos))
        val measureDoc = Document(measurePdf, receiptWidth)
        measureDoc.setMargins(topM, rightM, bottomM, leftM)
        renderContent(measureDoc, order)
        val rem = measureDoc.renderer.currentArea.bBox
        val contentHeight = (receiptWidth.height - topM - bottomM) - rem.height
        measureDoc.close()

        val finalHeight = contentHeight + topM + bottomM + 5f
        val baos = ByteArrayOutputStream()
        val pdfDoc = PdfDocument(PdfWriter(baos))
        pdfDoc.defaultPageSize = PageSize(255f, finalHeight)
        val document = Document(pdfDoc)
        document.setMargins(topM, rightM, bottomM, leftM)
        renderContent(document, order)
        document.close()

        return baos.toByteArray()
    }

    private fun renderContent(document: Document, order: Order) {
        val w = 255f - 20f
        addHeader(document, order, w)
        addDivider(document)
        addInvoiceTitle(document, order, w)
        addDivider(document)
        addOrderInfo(document, order)
        addProductsTable(document, order)
        addTotals(document, order)
        addTaxDetails(document, order)
        addAuthorizationText(document)
        addSignatureLine(document)
        addQrCode(document, order)
        addCufeSection(document, order)
        addDivider(document)
        addFooter(document, order)
    }

    private fun addHeader(document: Document, order: Order, w: Float) {
        if (logoBytes != null) {
            val imageData = ImageDataFactory.create(logoBytes)
            document.add(
                Image(imageData).setWidth(w * 0.55f)
                    .setHorizontalAlignment(HorizontalAlignment.CENTER).setMarginBottom(6f)
            )
        }
        document.add(centered(order.seller.name, 9f, true))
        document.add(centered(order.seller.email, 7f, false))
        document.add(centered("${order.shippingCity}, ${order.shippingState}", 7f, false).setMarginBottom(4f))
    }

    private fun addInvoiceTitle(document: Document, order: Order, w: Float) {
        document.add(centered("FACTURA ELECTRONICA DE VENTA", 8f, true).setMarginTop(2f))
        document.add(centered("No. KMP-${order.orderNumber}", 8f, true).setMarginBottom(4f))

        val barcode = generateBarcode("KMP-${order.orderNumber}", (w * 0.8f).toInt(), 30)
        if (barcode != null) {
            document.add(barcode.setWidth(w * 0.8f)
                .setHorizontalAlignment(HorizontalAlignment.CENTER).setMarginBottom(2f))
        }
    }

    private fun addOrderInfo(document: Document, order: Order) {
        val t = Table(UnitValue.createPercentArray(floatArrayOf(35f, 65f))).useAllAvailableWidth().setMarginBottom(2f)
        infoRow(t, "Fecha:", order.createdAt.format(dateFormatter))
        infoRow(t, "Hora:", order.createdAt.format(timeFormatter))
        document.add(t)

        document.add(leftBold("Cliente:").setMarginTop(3f))
        document.add(leftGray("  ${order.buyer.name}"))
        document.add(leftGray("  ${order.buyer.email}"))
        if (order.shippingCity.isNotBlank()) {
            document.add(leftGray("  ${order.shippingCity} - ${order.shippingState}"))
        }

        document.add(boldLabel("Forma de Pago:", if (order.paymentMethod == PaymentMethod.CASH_ON_DELIVERY) "Contado" else "Credito").setMarginTop(3f))
        document.add(boldLabel("Medio de Pago:", paymentLabel(order.paymentMethod)))
        document.add(boldLabel("Vendedor:", order.seller.name).setMarginBottom(4f))
    }

    private fun addProductsTable(document: Document, order: Order) {
        val t = Table(UnitValue.createPercentArray(floatArrayOf(10f, 50f, 10f, 30f)))
            .useAllAvailableWidth().setMarginBottom(1f)

        for (h in listOf("Cant", "Detalle", "Iva", "Total")) {
            t.addHeaderCell(Cell()
                .setBorderTop(SolidBorder(black, 0.8f)).setBorderBottom(SolidBorder(black, 0.8f))
                .setBorderLeft(Border.NO_BORDER).setBorderRight(Border.NO_BORDER)
                .setPadding(2f)
                .add(Paragraph(h).setFontSize(7f).setBold().setFontColor(darkText)
                    .setTextAlignment(if (h == "Detalle") TextAlignment.LEFT else TextAlignment.CENTER)))
        }

        for (item in order.items) {
            t.addCell(noB().add(Paragraph(item.quantity.toString()).setFontSize(6.5f).setFontColor(darkText).setTextAlignment(TextAlignment.CENTER)))

            val dc = noB()
            dc.add(Paragraph(item.productName).setFontSize(6.5f).setFontColor(darkText))
            if (!item.variantName.isNullOrBlank()) dc.add(Paragraph("- ${item.variantName}").setFontSize(6f).setFontColor(grayText))
            t.addCell(dc)

            t.addCell(noB().add(Paragraph("0").setFontSize(6.5f).setFontColor(darkText).setTextAlignment(TextAlignment.CENTER)))
            t.addCell(noB().add(Paragraph(cop(item.subtotal)).setFontSize(6.5f).setFontColor(darkText).setTextAlignment(TextAlignment.RIGHT)))
        }

        document.add(t)
    }

    private fun addTotals(document: Document, order: Order) {
        val t = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f))).useAllAvailableWidth().setMarginBottom(2f)

        totalRow(t, "Subtotal:", cop(order.subtotal))
        if (order.discount > BigDecimal.ZERO) totalRow(t, "Descuento:", "-${cop(order.discount)}")
        totalRow(t, "IVA (19%):", if (order.tax > BigDecimal.ZERO) cop(order.tax) else "$0")
        if (order.shipping > BigDecimal.ZERO) {
            if (order.paymentMethod == PaymentMethod.CASH_ON_DELIVERY) {
                val codFee = shippingConfig.codFee
                val shippingOnly = order.shipping.subtract(codFee)
                if (shippingOnly > BigDecimal.ZERO) totalRow(t, "Envio:", cop(shippingOnly))
                totalRow(t, "Recaudo contra entrega:", cop(codFee))
            } else {
                totalRow(t, "Envio:", cop(order.shipping))
            }
        }

        document.add(t)
        document.add(Paragraph("Total: ${cop(order.total)}").setFontSize(13f).setBold().setFontColor(darkText).setTextAlignment(TextAlignment.CENTER).setMarginBottom(1f))
        document.add(Paragraph("Cantidad items: ${order.items.size}").setFontSize(6.5f).setFontColor(grayText).setTextAlignment(TextAlignment.RIGHT).setMarginBottom(6f))
    }

    private fun addTaxDetails(document: Document, order: Order) {
        val t = Table(UnitValue.createPercentArray(floatArrayOf(33f, 34f, 33f))).useAllAvailableWidth().setMarginBottom(6f)

        t.addCell(Cell(1, 3).setBorder(SolidBorder(black, 0.5f)).setPadding(2f)
            .add(Paragraph("DETALLES DE IMPUESTOS").setFontSize(7f).setBold().setFontColor(darkText).setTextAlignment(TextAlignment.CENTER)))

        for (h in listOf("TARIFA", "BASE", "IMPUESTO")) {
            t.addCell(Cell().setBorder(SolidBorder(black, 0.5f)).setPadding(2f)
                .add(Paragraph(h).setFontSize(6f).setFontColor(darkText).setTextAlignment(TextAlignment.CENTER)))
        }

        t.addCell(Cell().setBorder(SolidBorder(black, 0.5f)).setPadding(2f).add(Paragraph("IVA 19%").setFontSize(6f).setFontColor(darkText)))
        t.addCell(Cell().setBorder(SolidBorder(black, 0.5f)).setPadding(2f).add(Paragraph(copNum(order.subtotal)).setFontSize(6f).setFontColor(darkText).setTextAlignment(TextAlignment.RIGHT)))
        t.addCell(Cell().setBorder(SolidBorder(black, 0.5f)).setPadding(2f).add(Paragraph(if (order.tax > BigDecimal.ZERO) copNum(order.tax) else "0").setFontSize(6f).setFontColor(darkText).setTextAlignment(TextAlignment.RIGHT)))

        document.add(t)
    }

    private fun addAuthorizationText(document: Document) {
        document.add(centered("RESPONSABLE DE IVA", 6.5f, true).setMarginBottom(0f))
        document.add(centered("RANGO KMP-00001 A KMP-99999", 5.5f, false))
        document.add(centered("AUTORIZACION NUMERACION DE FACTURACION", 5.5f, false))
        document.add(centered("No. 18764087188353 DEL 2026-01-01 VENCE 2028-01-01", 5.5f, false).setMarginBottom(8f))
    }

    private fun addSignatureLine(document: Document) {
        document.add(Paragraph().setMarginBottom(8f))
        document.add(Paragraph("Firma ___________________________________").setFontSize(7.5f).setFontColor(darkText).setMarginBottom(8f))
    }

    private fun addQrCode(document: Document, order: Order) {
        val qr = generateQrCode("https://kompralo.com/verificar/${order.orderNumber}", 120)
        if (qr != null) {
            document.add(qr.setWidth(80f).setHorizontalAlignment(HorizontalAlignment.CENTER).setMarginBottom(4f))
        }
    }

    private fun addCufeSection(document: Document, order: Order) {
        val cufe = generateCufe(order)
        document.add(centered("CUFE:", 7f, true).setMarginBottom(1f))
        val h = cufe.length / 2
        document.add(centered(cufe.substring(0, h), 5.5f, false).setMarginBottom(0f))
        document.add(centered(cufe.substring(h), 5.5f, false).setMarginBottom(6f))
    }

    private fun addFooter(document: Document, order: Order) {
        val datetime = order.createdAt.format(fullDateTimeFormatter)
        val t = Table(UnitValue.createPercentArray(floatArrayOf(30f, 70f))).useAllAvailableWidth().setMarginBottom(4f)
        metaRow(t, "Emision:", datetime)
        metaRow(t, "Expedicion:", datetime)
        metaRow(t, "Vendedor:", "${order.seller.name}")
        document.add(t)
        addDivider(document)
        document.add(centered("Software Kompralo E-Commerce 1.0", 6.5f, true).setMarginTop(2f))
        document.add(centered("www.kompralo.com", 6.5f, false))
    }

    private fun addDivider(document: Document) {
        document.add(Table(UnitValue.createPercentArray(floatArrayOf(100f))).useAllAvailableWidth()
            .addCell(Cell().setBorder(Border.NO_BORDER).setBorderBottom(SolidBorder(black, 0.5f)).setPadding(0f).setHeight(1f))
            .setMarginBottom(3f))
    }

    private fun infoRow(t: Table, l: String, v: String) {
        t.addCell(Cell().setBorder(Border.NO_BORDER).setPadding(1f).add(Paragraph(l).setFontSize(7f).setBold().setFontColor(darkText)))
        t.addCell(Cell().setBorder(Border.NO_BORDER).setPadding(1f).add(Paragraph(v).setFontSize(7f).setFontColor(darkText).setTextAlignment(TextAlignment.RIGHT)))
    }

    private fun totalRow(t: Table, l: String, v: String) {
        t.addCell(Cell().setBorder(Border.NO_BORDER).setPadding(1f).add(Paragraph(l).setFontSize(7.5f).setBold().setFontColor(darkText).setTextAlignment(TextAlignment.RIGHT)))
        t.addCell(Cell().setBorder(Border.NO_BORDER).setPadding(1f).add(Paragraph(v).setFontSize(7.5f).setFontColor(darkText).setTextAlignment(TextAlignment.RIGHT)))
    }

    private fun metaRow(t: Table, l: String, v: String) {
        t.addCell(Cell().setBorder(Border.NO_BORDER).setPadding(1f).add(Paragraph(l).setFontSize(6.5f).setBold().setFontColor(darkText)))
        t.addCell(Cell().setBorder(Border.NO_BORDER).setPadding(1f).add(Paragraph(v).setFontSize(6.5f).setFontColor(darkText)))
    }

    private fun centered(t: String, s: Float, b: Boolean): Paragraph {
        val p = Paragraph(t).setFontSize(s).setFontColor(darkText).setTextAlignment(TextAlignment.CENTER).setMarginBottom(1f)
        if (b) p.setBold()
        return p
    }

    private fun leftBold(t: String) = Paragraph(t).setFontSize(7.5f).setBold().setFontColor(darkText).setMarginBottom(1f)
    private fun leftGray(t: String) = Paragraph(t).setFontSize(7f).setFontColor(grayText).setMarginBottom(0f)
    private fun boldLabel(l: String, v: String) = Paragraph("$l $v").setFontSize(7f).setBold().setFontColor(darkText).setMarginBottom(0f)
    private fun noB() = Cell().setBorder(Border.NO_BORDER).setPadding(2f)

    private fun generateBarcode(text: String, width: Int, height: Int): Image? {
        return try {
            val m = Code128Writer().encode(text, BarcodeFormat.CODE_128, width, height)
            val baos = ByteArrayOutputStream()
            ImageIO.write(MatrixToImageWriter.toBufferedImage(m), "PNG", baos)
            Image(ImageDataFactory.create(baos.toByteArray()))
        } catch (_: Exception) { null }
    }

    private fun generateQrCode(text: String, size: Int): Image? {
        return try {
            val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.MARGIN to 1))
            val baos = ByteArrayOutputStream()
            ImageIO.write(MatrixToImageWriter.toBufferedImage(m), "PNG", baos)
            Image(ImageDataFactory.create(baos.toByteArray()))
        } catch (_: Exception) { null }
    }

    private fun generateCufe(order: Order): String {
        val raw = "KMP${order.orderNumber}${order.total}${order.createdAt}${order.buyer.email}${order.seller.email}"
        return MessageDigest.getInstance("SHA-384").digest(raw.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private fun paymentLabel(m: PaymentMethod?) = when (m) {
        PaymentMethod.CASH_ON_DELIVERY -> "Contra entrega"
        PaymentMethod.CREDIT_CARD -> "Tarjeta de Credito"
        PaymentMethod.DEBIT_CARD -> "Tarjeta de Debito"
        PaymentMethod.PSE -> "PSE"
        PaymentMethod.TRANSFER -> "Transferencia Bancaria"
        PaymentMethod.PAYPAL -> "PayPal"
        PaymentMethod.WOMPI -> "Wompi"
        null -> "No especificado"
    }

    private fun cop(a: BigDecimal): String = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply { maximumFractionDigits = 0 }.format(a)
    private fun copNum(a: BigDecimal): String = NumberFormat.getNumberInstance(Locale("es", "CO")).apply { maximumFractionDigits = 0 }.format(a)
}
