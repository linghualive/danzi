package com.live.commerce.mall.dto

import com.live.commerce.mall.entity.Order
import com.live.commerce.mall.entity.OrderItem
import java.math.BigDecimal
import java.time.LocalDateTime

data class OrderDTO(
    val id: Long,
    val orderNo: String,
    val userId: Long,
    val totalAmount: BigDecimal,
    val status: Int,
    val items: List<OrderItemDTO>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(order: Order, items: List<OrderItem>): OrderDTO = OrderDTO(
            id = order.id,
            orderNo = order.orderNo,
            userId = order.userId,
            totalAmount = order.totalAmount,
            status = order.status,
            items = items.map { OrderItemDTO.from(it) },
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
    val quantity: Int
) {
    companion object {
        fun from(item: OrderItem): OrderItemDTO = OrderItemDTO(
            id = item.id,
            productId = item.productId,
            productName = item.productName,
            price = item.price,
            quantity = item.quantity
        )
    }
}
