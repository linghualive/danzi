package com.live.commerce.mall.dto

import com.live.commerce.mall.entity.Order
import com.live.commerce.mall.entity.OrderItem
import java.math.BigDecimal
import java.time.LocalDateTime

data class OrderDTO(
    val id: Long,
    val orderNo: String,
    val buyerId: Long,
    val buyerName: String?,
    val sellerId: Long,
    val sellerName: String?,
    val totalAmount: BigDecimal,
    val status: Int,
    val items: List<OrderItemDTO>,
    val expireAt: LocalDateTime,
    val paidAt: LocalDateTime?,
    val refundReason: String?,
    val refundRequestedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(
            order: Order,
            items: List<OrderItem>,
            buyerName: String?,
            sellerName: String?
        ): OrderDTO = OrderDTO(
            id = order.id,
            orderNo = order.orderNo,
            buyerId = order.userId,
            buyerName = buyerName,
            sellerId = order.sellerId,
            sellerName = sellerName,
            totalAmount = order.totalAmount,
            status = order.status,
            items = items.map { OrderItemDTO.from(it) },
            expireAt = order.expireAt,
            paidAt = order.paidAt,
            refundReason = order.refundReason,
            refundRequestedAt = order.refundRequestedAt,
            createdAt = order.createdAt,
            updatedAt = order.updatedAt
        )
    }
}

data class OrderItemDTO(
    val id: Long,
    val productId: Long,
    val productName: String,
    val price: BigDecimal,
    val quantity: Int,
    val productImage: String? = null
) {
    companion object {
        fun from(item: OrderItem): OrderItemDTO = OrderItemDTO(
            id = item.id,
            productId = item.productId,
            productName = item.productName,
            price = item.price,
            quantity = item.quantity,
            productImage = item.productImage
        )
    }
}
