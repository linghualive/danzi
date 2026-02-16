package com.live.commerce.mall.service

import com.live.commerce.mall.dto.CreateOrderRequest
import com.live.commerce.mall.dto.OrderDTO

interface OrderService {

    fun createOrder(userId: Long, request: CreateOrderRequest): OrderDTO

    fun getOrder(orderId: Long, userId: Long): OrderDTO

    fun getOrderByOrderNo(orderNo: String): OrderDTO

    fun getUserOrders(userId: Long): List<OrderDTO>

    fun payOrder(orderId: Long, userId: Long): OrderDTO

    fun cancelOrder(orderId: Long, userId: Long): OrderDTO
}
