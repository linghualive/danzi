package com.live.commerce.mall.service

import com.live.commerce.mall.dto.CreateOrderRequest
import com.live.commerce.mall.dto.OrderDTO
import com.live.commerce.mall.dto.RefundRequest

interface OrderService {

    fun createOrder(userId: Long, request: CreateOrderRequest): OrderDTO

    fun getOrder(orderId: Long, userId: Long): OrderDTO

    fun getOrderByOrderNo(orderNo: String): OrderDTO

    fun getUserOrders(userId: Long): List<OrderDTO>

    fun getSoldOrders(userId: Long): List<OrderDTO>

    fun payOrder(orderId: Long, userId: Long): OrderDTO

    fun cancelOrder(orderId: Long, userId: Long): OrderDTO

    fun requestRefund(orderId: Long, userId: Long, request: RefundRequest): OrderDTO

    fun confirmRefund(orderId: Long, sellerId: Long): OrderDTO

    fun autoCancelExpiredOrders()
}
