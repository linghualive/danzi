package com.live.commerce.mall.service

import com.live.commerce.mall.dto.CreateOrderRequest
import com.live.commerce.mall.dto.LiveSummaryDTO
import com.live.commerce.mall.dto.OrderDTO
import com.live.commerce.mall.dto.RefundRequest
import java.time.LocalDateTime

interface OrderService {

    fun createOrder(userId: Long, request: CreateOrderRequest): OrderDTO

    fun getOrder(orderId: Long, userId: Long): OrderDTO

    fun getOrderByOrderNo(orderNo: String): OrderDTO

    fun getUserOrders(userId: Long, keyword: String? = null): List<OrderDTO>

    fun getSoldOrders(userId: Long, keyword: String? = null): List<OrderDTO>

    fun payOrder(orderId: Long, userId: Long): OrderDTO

    fun cancelOrder(orderId: Long, userId: Long): OrderDTO

    fun requestRefund(orderId: Long, userId: Long, request: RefundRequest): OrderDTO

    fun confirmRefund(orderId: Long, sellerId: Long): OrderDTO

    fun autoCancelExpiredOrders()

    fun getLiveSummary(sellerId: Long, from: LocalDateTime, to: LocalDateTime): LiveSummaryDTO
}
