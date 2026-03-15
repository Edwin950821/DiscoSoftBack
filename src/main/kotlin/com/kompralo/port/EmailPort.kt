package com.kompralo.port

import java.math.BigDecimal

interface EmailPort {

    fun sendHtmlEmailWithAttachment(
        to: String,
        subject: String,
        htmlContent: String,
        attachmentName: String? = null,
        attachmentBytes: ByteArray? = null
    ): Boolean

    fun sendOrderConfirmationToBuyer(
        buyerEmail: String,
        buyerName: String,
        orderNumber: String,
        total: BigDecimal,
        itemCount: Int,
        sellerName: String,
        pdfReceipt: ByteArray
    ): Boolean

    fun sendNewOrderNotificationToStore(
        sellerEmail: String,
        sellerName: String,
        orderNumber: String,
        buyerName: String,
        total: BigDecimal,
        itemCount: Int
    ): Boolean

    fun sendPasswordResetEmail(to: String, userName: String, resetToken: String, resetUrl: String): Boolean

    fun sendSellerWelcomeEmail(to: String, businessName: String): Boolean

    fun sendProblemReport(
        userEmail: String,
        userName: String,
        tipo: String,
        seccion: String,
        descripcion: String
    ): Boolean

    fun sendSellerVerifiedEmail(to: String, businessName: String): Boolean
}
