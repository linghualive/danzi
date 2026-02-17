package com.live.commerce.mall.service

import com.live.commerce.common.dto.Result
import com.live.commerce.common.dto.UserDTO
import com.live.commerce.common.exception.BusinessException
import com.live.commerce.common.exception.ErrorCode
import com.live.commerce.mall.dto.CreateOrderRequest
import com.live.commerce.mall.dto.OrderItemRequest
import com.live.commerce.mall.entity.Order
import com.live.commerce.mall.entity.OrderItem
import com.live.commerce.mall.entity.Product
import com.live.commerce.mall.feign.UserFeignClient
import com.live.commerce.mall.repository.OrderItemRepository
import com.live.commerce.mall.repository.OrderRepository
import com.live.commerce.mall.repository.ProductRepository
import com.live.commerce.mall.service.impl.OrderServiceImpl
import com.live.commerce.mall.support.UserPermissionSupport
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

class OrderServiceTest {

    private lateinit var orderRepository: OrderRepository
    private lateinit var orderItemRepository: OrderItemRepository
    private lateinit var productRepository: ProductRepository
    private lateinit var userFeignClient: UserFeignClient
    private lateinit var userPermissionSupport: UserPermissionSupport
    private lateinit var orderService: OrderService

    @BeforeEach
    fun setUp() {
        orderRepository = mockk()
        orderItemRepository = mockk()
        productRepository = mockk()
        userFeignClient = mockk()
        userPermissionSupport = UserPermissionSupport(userFeignClient)
        every { userFeignClient.getUserById(any()) } returns Result.ok(
            UserDTO(id = 1L, username = "testuser", nickname = "Test", avatar = null, role = 0)
        )
        orderService = OrderServiceImpl(orderRepository, orderItemRepository, productRepository, userFeignClient, userPermissionSupport)
    }

    @Test
    fun `should create order successfully`() {
        val userId = 1L
        val request = CreateOrderRequest(
            items = listOf(
                OrderItemRequest(productId = 1L, quantity = 2),
                OrderItemRequest(productId = 2L, quantity = 1)
            )
        )

        val userDTO = UserDTO(id = userId, username = "testuser", nickname = "Test", avatar = null, role = 0)
        every { userFeignClient.getUserById(userId) } returns Result.ok(userDTO)

        val product1 = Product(id = 1L, sellerId = 100L, name = "Product 1", price = BigDecimal("100.00"), stock = 10)
        val product2 = Product(id = 2L, sellerId = 100L, name = "Product 2", price = BigDecimal("50.00"), stock = 5)
        every { productRepository.findById(1L) } returns Optional.of(product1)
        every { productRepository.findById(2L) } returns Optional.of(product2)

        every { productRepository.deductStock(1L, 2) } returns 1
        every { productRepository.deductStock(2L, 1) } returns 1

        every { orderRepository.save(any()) } answers {
            val order = firstArg<Order>()
            order.apply { id = 1L }
        }

        every { orderItemRepository.saveAll(any<List<OrderItem>>()) } answers {
            val items = firstArg<List<OrderItem>>()
            items.mapIndexed { index, item -> item.apply { id = (index + 1).toLong() } }
        }

        every { orderItemRepository.findByOrderId(1L) } returns listOf(
            OrderItem(id = 1L, orderId = 1L, productId = 1L, productName = "Product 1", price = BigDecimal("100.00"), quantity = 2),
            OrderItem(id = 2L, orderId = 1L, productId = 2L, productName = "Product 2", price = BigDecimal("50.00"), quantity = 1)
        )

        val result = orderService.createOrder(userId, request)

        assertNotNull(result)
        assertEquals(1L, result.id)
        assertEquals(userId, result.buyerId)
        assertEquals(BigDecimal("250.00"), result.totalAmount)
        assertEquals(0, result.status)
        assertEquals(2, result.items.size)

        verify(atLeast = 1) { userFeignClient.getUserById(userId) }
        verify { productRepository.deductStock(1L, 2) }
        verify { productRepository.deductStock(2L, 1) }
        verify { orderRepository.save(any()) }
        verify { orderItemRepository.saveAll(any<List<OrderItem>>()) }
    }

    @Test
    fun `should throw exception when user not found via feign`() {
        val userId = 999L
        val request = CreateOrderRequest(
            items = listOf(OrderItemRequest(productId = 1L, quantity = 1))
        )

        every { userFeignClient.getUserById(userId) } returns Result.error(ErrorCode.USER_NOT_FOUND, "用户不存在")

        val exception = assertThrows<BusinessException> {
            orderService.createOrder(userId, request)
        }
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.code)
    }

    @Test
    fun `should throw exception when product not found`() {
        val userId = 1L
        val request = CreateOrderRequest(
            items = listOf(OrderItemRequest(productId = 999L, quantity = 1))
        )

        val userDTO = UserDTO(id = userId, username = "testuser", nickname = "Test", avatar = null, role = 0)
        every { userFeignClient.getUserById(userId) } returns Result.ok(userDTO)
        every { productRepository.findById(999L) } returns Optional.empty()

        val exception = assertThrows<BusinessException> {
            orderService.createOrder(userId, request)
        }
        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, exception.code)
    }

    @Test
    fun `should throw exception when stock not enough`() {
        val userId = 1L
        val request = CreateOrderRequest(
            items = listOf(OrderItemRequest(productId = 1L, quantity = 100))
        )

        val userDTO = UserDTO(id = userId, username = "testuser", nickname = "Test", avatar = null, role = 0)
        every { userFeignClient.getUserById(userId) } returns Result.ok(userDTO)

        val product = Product(id = 1L, sellerId = 100L, name = "Product 1", price = BigDecimal("100.00"), stock = 5)
        every { productRepository.findById(1L) } returns Optional.of(product)
        every { productRepository.deductStock(1L, 100) } returns 0

        val exception = assertThrows<BusinessException> {
            orderService.createOrder(userId, request)
        }
        assertEquals(ErrorCode.STOCK_NOT_ENOUGH, exception.code)
    }

    @Test
    fun `should generate unique order number`() {
        val userId = 1L
        val request = CreateOrderRequest(
            items = listOf(OrderItemRequest(productId = 1L, quantity = 1))
        )

        val userDTO = UserDTO(id = userId, username = "testuser", nickname = "Test", avatar = null, role = 0)
        every { userFeignClient.getUserById(userId) } returns Result.ok(userDTO)

        val product = Product(id = 1L, sellerId = 100L, name = "Product 1", price = BigDecimal("100.00"), stock = 10)
        every { productRepository.findById(1L) } returns Optional.of(product)
        every { productRepository.deductStock(1L, 1) } returns 1

        every { orderRepository.save(any()) } answers {
            val order = firstArg<Order>()
            order.apply { id = 1L }
        }

        every { orderItemRepository.saveAll(any<List<OrderItem>>()) } answers {
            firstArg<List<OrderItem>>().mapIndexed { index, item -> item.apply { id = (index + 1).toLong() } }
        }

        every { orderItemRepository.findByOrderId(1L) } returns listOf(
            OrderItem(id = 1L, orderId = 1L, productId = 1L, productName = "Product 1", price = BigDecimal("100.00"), quantity = 1)
        )

        val result = orderService.createOrder(userId, request)

        assertNotNull(result.orderNo)
        assertTrue(result.orderNo.isNotBlank())
        assertTrue(result.orderNo.length >= 18) // yyyyMMddHHmmss (14) + 4 digit sequence
    }

    @Test
    fun `should calculate total amount correctly`() {
        val userId = 1L
        val request = CreateOrderRequest(
            items = listOf(
                OrderItemRequest(productId = 1L, quantity = 3),
                OrderItemRequest(productId = 2L, quantity = 2)
            )
        )

        val userDTO = UserDTO(id = userId, username = "testuser", nickname = "Test", avatar = null, role = 0)
        every { userFeignClient.getUserById(userId) } returns Result.ok(userDTO)

        val product1 = Product(id = 1L, sellerId = 100L, name = "Product 1", price = BigDecimal("10.50"), stock = 10)
        val product2 = Product(id = 2L, sellerId = 100L, name = "Product 2", price = BigDecimal("20.00"), stock = 5)
        every { productRepository.findById(1L) } returns Optional.of(product1)
        every { productRepository.findById(2L) } returns Optional.of(product2)
        every { productRepository.deductStock(1L, 3) } returns 1
        every { productRepository.deductStock(2L, 2) } returns 1

        every { orderRepository.save(any()) } answers {
            val order = firstArg<Order>()
            order.apply { id = 1L }
        }

        every { orderItemRepository.saveAll(any<List<OrderItem>>()) } answers {
            firstArg<List<OrderItem>>().mapIndexed { index, item -> item.apply { id = (index + 1).toLong() } }
        }

        every { orderItemRepository.findByOrderId(1L) } returns listOf(
            OrderItem(id = 1L, orderId = 1L, productId = 1L, productName = "Product 1", price = BigDecimal("10.50"), quantity = 3),
            OrderItem(id = 2L, orderId = 1L, productId = 2L, productName = "Product 2", price = BigDecimal("20.00"), quantity = 2)
        )

        val result = orderService.createOrder(userId, request)

        // 10.50 * 3 + 20.00 * 2 = 31.50 + 40.00 = 71.50
        assertEquals(BigDecimal("71.50"), result.totalAmount)
    }

    @Test
    fun `should get order by id`() {
        val order = Order(
            id = 1L,
            orderNo = "20240101120000001",
            userId = 1L,
            sellerId = 100L,
            totalAmount = BigDecimal("100.00"),
            status = 0
        )
        val items = listOf(
            OrderItem(id = 1L, orderId = 1L, productId = 1L, productName = "Product 1", price = BigDecimal("100.00"), quantity = 1)
        )

        every { orderRepository.findById(1L) } returns Optional.of(order)
        every { orderItemRepository.findByOrderId(1L) } returns items

        val result = orderService.getOrder(1L, 1L)

        assertNotNull(result)
        assertEquals(1L, result.id)
        assertEquals("20240101120000001", result.orderNo)
        assertEquals(BigDecimal("100.00"), result.totalAmount)
        assertEquals(1, result.items.size)
    }

    @Test
    fun `should throw exception when order not found`() {
        every { orderRepository.findById(999L) } returns Optional.empty()

        val exception = assertThrows<BusinessException> {
            orderService.getOrder(999L, 1L)
        }
        assertEquals(ErrorCode.ORDER_NOT_FOUND, exception.code)
    }

    @Test
    fun `should pay order successfully`() {
        val order = Order(
            id = 1L,
            orderNo = "20240101120000001",
            userId = 1L,
            sellerId = 100L,
            totalAmount = BigDecimal("100.00"),
            status = 0
        )
        val items = listOf(
            OrderItem(id = 1L, orderId = 1L, productId = 1L, productName = "Product 1", price = BigDecimal("100.00"), quantity = 1)
        )

        every { orderRepository.findById(1L) } returns Optional.of(order)
        every { orderRepository.save(any()) } answers { firstArg() }
        every { orderItemRepository.findByOrderId(1L) } returns items

        val result = orderService.payOrder(1L, 1L)

        assertEquals(1, result.status)
        verify { orderRepository.save(match { it.status == 1 }) }
    }

    @Test
    fun `should throw exception when paying non-pending order`() {
        val order = Order(
            id = 1L,
            orderNo = "20240101120000001",
            userId = 1L,
            sellerId = 100L,
            totalAmount = BigDecimal("100.00"),
            status = 1 // already paid
        )

        every { orderRepository.findById(1L) } returns Optional.of(order)

        val exception = assertThrows<BusinessException> {
            orderService.payOrder(1L, 1L)
        }
        assertEquals(ErrorCode.ORDER_STATUS_ERROR, exception.code)
    }

    @Test
    fun `should cancel order successfully`() {
        val order = Order(
            id = 1L,
            orderNo = "20240101120000001",
            userId = 1L,
            sellerId = 100L,
            totalAmount = BigDecimal("100.00"),
            status = 0
        )
        val items = listOf(
            OrderItem(id = 1L, orderId = 1L, productId = 1L, productName = "Product 1", price = BigDecimal("100.00"), quantity = 2),
            OrderItem(id = 2L, orderId = 1L, productId = 2L, productName = "Product 2", price = BigDecimal("50.00"), quantity = 1)
        )

        every { orderRepository.findById(1L) } returns Optional.of(order)
        every { orderItemRepository.findByOrderId(1L) } returns items
        every { productRepository.restoreStock(1L, 2) } returns 1
        every { productRepository.restoreStock(2L, 1) } returns 1
        every { orderRepository.save(any()) } answers { firstArg() }

        val result = orderService.cancelOrder(1L, 1L)

        assertEquals(2, result.status)
        verify { productRepository.restoreStock(1L, 2) }
        verify { productRepository.restoreStock(2L, 1) }
        verify { orderRepository.save(match { it.status == 2 }) }
    }

    @Test
    fun `should throw exception when cancelling non-pending order`() {
        val order = Order(
            id = 1L,
            orderNo = "20240101120000001",
            userId = 1L,
            sellerId = 100L,
            totalAmount = BigDecimal("100.00"),
            status = 1 // already paid
        )

        every { orderRepository.findById(1L) } returns Optional.of(order)

        val exception = assertThrows<BusinessException> {
            orderService.cancelOrder(1L, 1L)
        }
        assertEquals(ErrorCode.ORDER_STATUS_ERROR, exception.code)
    }

    @Test
    fun `should throw exception when user has no permission to operate order`() {
        val order = Order(
            id = 1L,
            orderNo = "20240101120000001",
            userId = 1L,
            sellerId = 100L,
            totalAmount = BigDecimal("100.00"),
            status = 0
        )

        every { orderRepository.findById(1L) } returns Optional.of(order)

        // Try to pay with different userId
        val payException = assertThrows<BusinessException> {
            orderService.payOrder(1L, 999L) // userId 999 is not the owner
        }
        assertEquals(ErrorCode.FORBIDDEN, payException.code)

        // Try to cancel with different userId
        val cancelException = assertThrows<BusinessException> {
            orderService.cancelOrder(1L, 999L) // userId 999 is not the owner
        }
        assertEquals(ErrorCode.FORBIDDEN, cancelException.code)
    }

    @Test
    fun `should request refund for paid order`() {
        val order = Order(
            id = 1L,
            orderNo = "20240101120000001",
            userId = 1L,
            sellerId = 100L,
            totalAmount = BigDecimal("100.00"),
            status = 1
        )
        every { orderRepository.findById(1L) } returns Optional.of(order)
        every { orderRepository.save(any()) } answers { firstArg() }
        every { orderItemRepository.findByOrderId(1L) } returns emptyList()

        val result = orderService.requestRefund(
            1L,
            1L,
            com.live.commerce.mall.dto.RefundRequest(reason = "商品与描述不符")
        )

        assertEquals(3, result.status)
        assertEquals("商品与描述不符", result.refundReason)
        assertNotNull(result.refundRequestedAt)
        verify { orderRepository.save(match { it.status == 3 && it.refundReason == "商品与描述不符" }) }
    }

    @Test
    fun `should confirm refund by seller`() {
        val order = Order(
            id = 1L,
            orderNo = "20240101120000001",
            userId = 1L,
            sellerId = 100L,
            totalAmount = BigDecimal("100.00"),
            status = 3,
            refundReason = "商品与描述不符",
            refundRequestedAt = LocalDateTime.now().minusMinutes(2)
        )
        val items = listOf(
            OrderItem(id = 1L, orderId = 1L, productId = 7L, productName = "Product 1", price = BigDecimal("100.00"), quantity = 1)
        )
        every { orderRepository.findById(1L) } returns Optional.of(order)
        every { orderItemRepository.findByOrderId(1L) } returns items
        every { productRepository.restoreStock(7L, 1) } returns 1
        every { orderRepository.save(any()) } answers { firstArg() }

        val result = orderService.confirmRefund(1L, 100L)

        assertEquals(4, result.status)
        verify { productRepository.restoreStock(7L, 1) }
        verify { orderRepository.save(match { it.status == 4 }) }
    }

    @Test
    fun `should reject refund confirmation by non seller`() {
        val order = Order(
            id = 1L,
            orderNo = "20240101120000001",
            userId = 1L,
            sellerId = 100L,
            totalAmount = BigDecimal("100.00"),
            status = 3
        )
        every { orderRepository.findById(1L) } returns Optional.of(order)

        val exception = assertThrows<BusinessException> {
            orderService.confirmRefund(1L, 999L)
        }
        assertEquals(ErrorCode.FORBIDDEN, exception.code)
    }

    @Test
    fun `should reject refund request when reason is blank`() {
        val order = Order(
            id = 1L,
            orderNo = "20240101120000001",
            userId = 1L,
            sellerId = 100L,
            totalAmount = BigDecimal("100.00"),
            status = 1
        )
        every { orderRepository.findById(1L) } returns Optional.of(order)

        val exception = assertThrows<BusinessException> {
            orderService.requestRefund(1L, 1L, com.live.commerce.mall.dto.RefundRequest("   "))
        }
        assertEquals(ErrorCode.REFUND_REASON_REQUIRED, exception.code)
    }

    @Test
    fun `should return sold orders with paid or refund related status only`() {
        val pending = Order(
            id = 1L,
            orderNo = "20240101120000001",
            userId = 10L,
            sellerId = 100L,
            totalAmount = BigDecimal("10.00"),
            status = 0
        )
        val paid = Order(
            id = 2L,
            orderNo = "20240101120000002",
            userId = 11L,
            sellerId = 100L,
            totalAmount = BigDecimal("20.00"),
            status = 1
        )
        val refundRequested = Order(
            id = 3L,
            orderNo = "20240101120000003",
            userId = 12L,
            sellerId = 100L,
            totalAmount = BigDecimal("30.00"),
            status = 3
        )
        val refunded = Order(
            id = 4L,
            orderNo = "20240101120000004",
            userId = 13L,
            sellerId = 100L,
            totalAmount = BigDecimal("40.00"),
            status = 4
        )
        every { orderRepository.findBySellerId(100L) } returns listOf(pending, paid, refundRequested, refunded)
        every { orderItemRepository.findByOrderId(any()) } returns emptyList()

        val result = orderService.getSoldOrders(100L)

        assertEquals(3, result.size)
        assertTrue(result.all { it.status == 1 || it.status == 3 || it.status == 4 })
        assertFalse(result.any { it.status == 0 })
    }

    @Test
    fun `should auto cancel expired orders and restore stock`() {
        val expired = Order(
            id = 1L,
            orderNo = "20240101120000001",
            userId = 1L,
            sellerId = 100L,
            totalAmount = BigDecimal("100.00"),
            status = 0,
            expireAt = LocalDateTime.now().minusMinutes(1)
        )
        val items = listOf(
            OrderItem(
                id = 1L,
                orderId = 1L,
                productId = 7L,
                productName = "Expired Product",
                price = BigDecimal("100.00"),
                quantity = 2
            )
        )
        every { orderRepository.findByStatusAndExpireAtBefore(eq(0), any()) } returns listOf(expired)
        every { orderItemRepository.findByOrderId(1L) } returns items
        every { productRepository.restoreStock(7L, 2) } returns 1
        every { orderRepository.save(any()) } answers { firstArg() }

        orderService.autoCancelExpiredOrders()

        verify { productRepository.restoreStock(7L, 2) }
        verify { orderRepository.save(match { it.id == 1L && it.status == 2 }) }
    }

    @Test
    fun `should fail pay order when expired and cancel it`() {
        val expired = Order(
            id = 1L,
            orderNo = "20240101120000001",
            userId = 1L,
            sellerId = 100L,
            totalAmount = BigDecimal("100.00"),
            status = 0,
            expireAt = LocalDateTime.now().minusSeconds(5)
        )
        val items = listOf(
            OrderItem(
                id = 1L,
                orderId = 1L,
                productId = 9L,
                productName = "Expired Product",
                price = BigDecimal("100.00"),
                quantity = 1
            )
        )
        every { orderRepository.findById(1L) } returns Optional.of(expired)
        every { orderItemRepository.findByOrderId(1L) } returns items
        every { productRepository.restoreStock(9L, 1) } returns 1
        every { orderRepository.save(any()) } answers { firstArg() }

        val exception = assertThrows<BusinessException> {
            orderService.payOrder(1L, 1L)
        }

        assertEquals(ErrorCode.ORDER_EXPIRED, exception.code)
        verify { orderRepository.save(match { it.id == 1L && it.status == 2 }) }
    }
}
