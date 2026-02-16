package com.live.commerce.mall.repository

import com.live.commerce.mall.entity.OrderItem
import org.springframework.data.jpa.repository.JpaRepository

interface OrderItemRepository : JpaRepository<OrderItem, Long> {

    fun findByOrderId(orderId: Long): List<OrderItem>
}
