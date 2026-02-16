package com.live.commerce.mall.service.impl

import com.live.commerce.common.exception.BusinessException
import com.live.commerce.common.exception.ErrorCode
import com.live.commerce.common.util.OrderNoGenerator
import com.live.commerce.mall.dto.CreateOrderRequest
import com.live.commerce.mall.dto.OrderDTO
import com.live.commerce.mall.entity.Order
import com.live.commerce.mall.entity.OrderItem
import com.live.commerce.mall.feign.UserFeignClient
import com.live.commerce.mall.repository.OrderItemRepository
import com.live.commerce.mall.repository.OrderRepository
import com.live.commerce.mall.repository.ProductRepository
import com.live.commerce.mall.service.OrderService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class OrderServiceImpl(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val productRepository: ProductRepository,
    private val userFeignClient: UserFeignClient
) : OrderService {

    @Transactional
    override fun createOrder(userId: Long, request: CreateOrderRequest): OrderDTO {
        // 1. Verify user exists via Feign
        val userResult = userFeignClient.getUserById(userId)
        if (userResult.code != 200 || userResult.data == null) {
            throw BusinessException(ErrorCode.USER_NOT_FOUND)
        }

        // 2. Process items: validate products and deduct stock
        var totalAmount = BigDecimal.ZERO
        val orderItems = mutableListOf<OrderItem>()

        for (item in request.items) {
            val product = productRepository.findById(item.productId)
                .orElseThrow { BusinessException(ErrorCode.PRODUCT_NOT_FOUND) }

            // Atomic stock deduction
            val updatedRows = productRepository.deductStock(item.productId, item.quantity)
            if (updatedRows == 0) {
                throw BusinessException(ErrorCode.STOCK_NOT_ENOUGH)
            }

            val itemAmount = product.price.multiply(BigDecimal(item.quantity))
            totalAmount = totalAmount.add(itemAmount)

            orderItems.add(
                OrderItem(
                    productId = item.productId,
                    productName = product.name,
                    price = product.price,
                    quantity = item.quantity
                )
            )
        }

        // 3. Generate order number and save order
        val order = Order(
            orderNo = OrderNoGenerator.generate(),
            userId = userId,
            totalAmount = totalAmount,
            status = 0
        )
        val savedOrder = orderRepository.save(order)

        // 4. Save order items
        orderItems.forEach { it.orderId = savedOrder.id }
        orderItemRepository.saveAll(orderItems)

        // 5. Return DTO
        val savedItems = orderItemRepository.findByOrderId(savedOrder.id)
        return OrderDTO.from(savedOrder, savedItems)
    }

    override fun getOrder(orderId: Long, userId: Long): OrderDTO {
        val order = orderRepository.findById(orderId)
            .orElseThrow { BusinessException(ErrorCode.ORDER_NOT_FOUND) }
        if (order.userId != userId) {
            throw BusinessException(ErrorCode.FORBIDDEN)
        }
        val items = orderItemRepository.findByOrderId(order.id)
        return OrderDTO.from(order, items)
    }

    override fun getOrderByOrderNo(orderNo: String): OrderDTO {
        val order = orderRepository.findByOrderNo(orderNo)
            ?: throw BusinessException(ErrorCode.ORDER_NOT_FOUND)
        val items = orderItemRepository.findByOrderId(order.id)
        return OrderDTO.from(order, items)
    }

    override fun getUserOrders(userId: Long): List<OrderDTO> {
        val orders = orderRepository.findByUserId(userId)
        return orders.map { order ->
            val items = orderItemRepository.findByOrderId(order.id)
            OrderDTO.from(order, items)
        }
    }

    @Transactional
    override fun payOrder(orderId: Long, userId: Long): OrderDTO {
        val order = orderRepository.findById(orderId)
            .orElseThrow { BusinessException(ErrorCode.ORDER_NOT_FOUND) }

        // Verify user permission
        if (order.userId != userId) {
            throw BusinessException(ErrorCode.FORBIDDEN)
        }

        // Verify status: only pending orders can be paid
        if (order.status != 0) {
            throw BusinessException(ErrorCode.ORDER_STATUS_ERROR)
        }

        order.status = 1
        order.updatedAt = LocalDateTime.now()
        orderRepository.save(order)

        val items = orderItemRepository.findByOrderId(order.id)
        return OrderDTO.from(order, items)
    }

    @Transactional
    override fun cancelOrder(orderId: Long, userId: Long): OrderDTO {
        val order = orderRepository.findById(orderId)
            .orElseThrow { BusinessException(ErrorCode.ORDER_NOT_FOUND) }

        // Verify user permission
        if (order.userId != userId) {
            throw BusinessException(ErrorCode.FORBIDDEN)
        }

        // Verify status: only pending orders can be cancelled
        if (order.status != 0) {
            throw BusinessException(ErrorCode.ORDER_STATUS_ERROR)
        }

        // Restore stock for each item
        val items = orderItemRepository.findByOrderId(order.id)
        for (item in items) {
            productRepository.restoreStock(item.productId, item.quantity)
        }

        order.status = 2
        order.updatedAt = LocalDateTime.now()
        orderRepository.save(order)

        return OrderDTO.from(order, items)
    }
}
