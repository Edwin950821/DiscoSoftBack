package com.kompralo.port

import com.kompralo.model.Order

interface PdfPort {

    fun generateReceiptById(orderId: Long): ByteArray?

    fun generateReceipt(order: Order): ByteArray
}
