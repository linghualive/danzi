package com.live.commerce.mall.service.impl

import com.live.commerce.common.exception.BusinessException
import com.live.commerce.common.exception.ErrorCode
import com.live.commerce.common.util.OrderNoGenerator
import com.live.commerce.mall.dto.CreateOrderRequest
import com.live.commerce.mall.dto.OrderDTO
import com.live.commerce.mall.dto.RefundRequest
import com.live.commerce.mall.entity.Order
import com.live.commerce.mall.entity.OrderItem
import com.live.commerce.mall.feign.UserFeignClient
import com.live.commerce.mall.repository.OrderItemRepository
import com.live.commerce.mall.repository.OrderRepository
import com.live.commerce.mall.repository.ProductRepository
import com.live.commerce.mall.service.OrderService
import com.live.commerce.mall.support.UserPermissionSupport
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class OrderServiceImpl(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val productRepository: ProductRepository,
    private val userFeignClient: UserFeignClient,
    private val userPermissionSupport: UserPermissionSupport
) : OrderService {

    @Transactional
    override fun createOrder(userId: Long, request: CreateOrderRequest): OrderDTO {
        userPermissionSupport.getUser(userId)

        var totalAmount = BigDecimal.ZERO
        val orderItems = mutableListOf<OrderItem>()
        var sellerId: Long? = null

        for (item in request.items) {
            val product = productRepository.findById(item.productId)
                .orElseThrow { BusinessException(ErrorCode.PRODUCT_NOT_FOUND) }

            if (product.status != 1) {
                throw BusinessException(ErrorCode.ORDER_STATUS_ERROR, "商品已下架")
            }

            if (sellerId == null) {
                sellerId = product.sellerId
            } else if (sellerId != product.sellerId) {
                throw BusinessException(ErrorCode.PARAM_ERROR, "单笔订单仅支持同一卖家商品")
            }

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

        val resolvedSellerId = sellerId ?: throw BusinessException(ErrorCode.PARAM_ERROR)
        val order = Order(
            orderNo = OrderNoGenerator.generate(),
            userId = userId,
            sellerId = resolvedSellerId,
            totalAmount = totalAmount,
            status = 0,
            expireAt = LocalDateTime.now().plusMinutes(5)
        )
        val savedOrder = orderRepository.save(order)

        orderItems.forEach { it.orderId = savedOrder.id }
        orderItemRepository.saveAll(orderItems)
        return buildOrderDTO(savedOrder)
    }

    override fun getOrder(orderId: Long, userId: Long): OrderDTO {
        val order = orderRepository.findById(orderId)
            .orElseThrow { BusinessException(ErrorCode.ORDER_NOT_FOUND) }
        userPermissionSupport.ensureOrderAccessor(userId, order.userId, order.sellerId)
        return buildOrderDTO(order)
    }

    override fun getOrderByOrderNo(orderNo: String): OrderDTO {
        val order = orderRepository.findByOrderNo(orderNo)
            ?: throw BusinessException(ErrorCode.ORDER_NOT_FOUND)
        return buildOrderDTO(order)
    }

    override fun getUserOrders(userId: Long): List<OrderDTO> {
        return orderRepository.findByUserId(userId)
            .sortedByDescending { it.createdAt }
            .map { buildOrderDTO(it) }
    }

    override fun getSoldOrders(userId: Long): List<OrderDTO> {
        return orderRepository.findBySellerId(userId)
            .filter { it.status == 1 || it.status == 3 }
            .sortedByDescending { it.createdAt }
            .map { buildOrderDTO(it) }
    }

    @Transactional
    override fun payOrder(orderId: Long, userId: Long): OrderDTO {
        val order = orderRepository.findById(orderId)
            .orElseThrow { BusinessException(ErrorCode.ORDER_NOT_FOUND) }

        if (order.userId != userId) {
            throw BusinessException(ErrorCode.FORBIDDEN)
        }

        if (order.status != 0) {
            throw BusinessException(ErrorCode.ORDER_STATUS_ERROR)
        }

        if (LocalDateTime.now().isAfter(order.expireAt)) {
            cancelPendingOrder(order)
            throw BusinessException(ErrorCode.ORDER_EXPIRED)
        }

        order.status = 1
        order.paidAt = LocalDateTime.now()
        order.updatedAt = LocalDateTime.now()
        orderRepository.save(order)

        return buildOrderDTO(order)
    }

    @Transactional
    override fun cancelOrder(orderId: Long, userId: Long): OrderDTO {
        val order = orderRepository.findById(orderId)
            .orElseThrow { BusinessException(ErrorCode.ORDER_NOT_FOUND) }

        if (order.userId != userId) {
            throw BusinessException(ErrorCode.FORBIDDEN)
        }

        if (order.status != 0) {
            throw BusinessException(ErrorCode.ORDER_STATUS_ERROR)
        }

        cancelPendingOrder(order)
        return buildOrderDTO(order)
    }

    @Transactional
    override fun requestRefund(orderId: Long, userId: Long, request: RefundRequest): OrderDTO {
        val order = orderRepository.findById(orderId)
            .orElseThrow { BusinessException(ErrorCode.ORDER_NOT_FOUND) }
        if (order.userId != userId) {
            throw BusinessException(ErrorCode.FORBIDDEN)
        }
        if (request.reason.isBlank()) {
            throw BusinessException(ErrorCode.REFUND_REASON_REQUIRED)
        }
        if (order.status != 1) {
            throw BusinessException(ErrorCode.ORDER_STATUS_ERROR, "仅已支付订单可申请退款")
        }

        order.status = 3
        order.refundReason = request.reason.trim()
        order.refundRequestedAt = LocalDateTime.now()
        order.updatedAt = LocalDateTime.now()
        orderRepository.save(order)
        return buildOrderDTO(order)
    }

    @Transactional
    override fun autoCancelExpiredOrders() {
        val expiredOrders = orderRepository.findByStatusAndExpireAtBefore(0, LocalDateTime.now())
        expiredOrders.forEach { cancelPendingOrder(it) }
    }

    private fun cancelPendingOrder(order: Order) {
        val items = orderItemRepository.findByOrderId(order.id)
        for (item in items) {
            productRepository.restoreStock(item.productId, item.quantity)
        }

        order.status = 2
        order.updatedAt = LocalDateTime.now()
        orderRepository.save(order)
    }

    private fun buildOrderDTO(order: Order): OrderDTO {
        val items = orderItemRepository.findByOrderId(order.id)
        val buyerName = resolveUserName(order.userId)
        val sellerName = resolveUserName(order.sellerId)
        return OrderDTO.from(order, items, buyerName, sellerName)
    }

    private fun resolveUserName(userId: Long): String? {
        return try {
            userFeignClient.getUserById(userId).data?.nickname
        } catch (_: Exception) {
            null
        }
    }
}
