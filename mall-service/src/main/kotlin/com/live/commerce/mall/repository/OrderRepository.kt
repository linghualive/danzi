package com.live.commerce.mall.repository

import com.live.commerce.mall.entity.Order
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface OrderRepository : JpaRepository<Order, Long> {

    fun findByOrderNo(orderNo: String): Order?

    fun findByUserId(userId: Long): List<Order>

    fun findBySellerId(sellerId: Long): List<Order>

    fun findByStatusAndExpireAtBefore(status: Int, expireAt: LocalDateTime): List<Order>

    @Query("""
        SELECT DISTINCT o FROM Order o LEFT JOIN OrderItem oi ON o.id = oi.orderId
        WHERE o.userId = :userId AND (o.orderNo LIKE :keyword OR oi.productName LIKE :keyword)
        ORDER BY o.createdAt DESC
    """)
    fun findByUserIdAndKeyword(@Param("userId") userId: Long, @Param("keyword") keyword: String): List<Order>

    @Query("""
        SELECT DISTINCT o FROM Order o LEFT JOIN OrderItem oi ON o.id = oi.orderId
        WHERE o.sellerId = :sellerId AND (o.orderNo LIKE :keyword OR oi.productName LIKE :keyword)
        ORDER BY o.createdAt DESC
    """)
    fun findBySellerIdAndKeyword(@Param("sellerId") sellerId: Long, @Param("keyword") keyword: String): List<Order>

    fun findBySellerIdAndStatusInAndCreatedAtBetween(
        sellerId: Long,
        statuses: List<Int>,
        from: LocalDateTime,
        to: LocalDateTime
    ): List<Order>
}
