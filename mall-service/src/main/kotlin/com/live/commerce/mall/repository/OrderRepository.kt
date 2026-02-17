package com.live.commerce.mall.repository

import com.live.commerce.mall.entity.Order
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface OrderRepository : JpaRepository<Order, Long> {

    fun findByOrderNo(orderNo: String): Order?

    fun findByUserId(userId: Long): List<Order>

    fun findBySellerId(sellerId: Long): List<Order>

    fun findByStatusAndExpireAtBefore(status: Int, expireAt: LocalDateTime): List<Order>
}
