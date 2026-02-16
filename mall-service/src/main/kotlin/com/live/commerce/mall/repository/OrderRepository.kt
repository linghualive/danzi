package com.live.commerce.mall.repository

import com.live.commerce.mall.entity.Order
import org.springframework.data.jpa.repository.JpaRepository

interface OrderRepository : JpaRepository<Order, Long> {

    fun findByOrderNo(orderNo: String): Order?

    fun findByUserId(userId: Long): List<Order>
}
