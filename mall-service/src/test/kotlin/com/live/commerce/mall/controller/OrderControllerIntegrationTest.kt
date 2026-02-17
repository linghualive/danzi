package com.live.commerce.mall.controller

import cn.dev33.satoken.stp.StpUtil
import com.fasterxml.jackson.databind.ObjectMapper
import com.live.commerce.common.dto.Result
import com.live.commerce.common.dto.UserDTO
import com.live.commerce.common.exception.ErrorCode
import com.live.commerce.mall.TestcontainersConfig
import com.live.commerce.mall.dto.CreateOrderRequest
import com.live.commerce.mall.dto.OrderItemRequest
import com.live.commerce.mall.entity.Product
import com.live.commerce.mall.feign.UserFeignClient
import com.live.commerce.mall.repository.OrderItemRepository
import com.live.commerce.mall.repository.OrderRepository
import com.live.commerce.mall.repository.ProductRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.math.BigDecimal

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerIntegrationTest : TestcontainersConfig() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var productRepository: ProductRepository

    @Autowired
    lateinit var orderRepository: OrderRepository

    @Autowired
    lateinit var orderItemRepository: OrderItemRepository

    @MockBean
    lateinit var userFeignClient: UserFeignClient

    private var saToken: String = ""
    private var buyerToken: String = ""
    private val testUserId = 1L
    private val buyerUserId = 2L

    @BeforeEach
    fun setUp() {
        orderItemRepository.deleteAll()
        orderRepository.deleteAll()
        productRepository.deleteAll()

        // Login via Sa-Token and obtain the token
        StpUtil.login(testUserId)
        saToken = StpUtil.getTokenValue()
        StpUtil.login(buyerUserId)
        buyerToken = StpUtil.getTokenValue()

        // Mock the Feign client to return a valid user
        val userDTO = UserDTO(id = testUserId, username = "testuser", nickname = "Test User", avatar = null, role = 0)
        Mockito.`when`(userFeignClient.getUserById(testUserId)).thenReturn(Result.ok(userDTO))
        val buyerDTO = UserDTO(id = buyerUserId, username = "buyer", nickname = "Buyer User", avatar = null, role = 0)
        Mockito.`when`(userFeignClient.getUserById(buyerUserId)).thenReturn(Result.ok(buyerDTO))
    }

    @Test
    fun `should create order successfully`() {
        // Create test products
        val product1 = productRepository.save(
            Product(name = "Product 1", price = BigDecimal("100.00"), stock = 10)
        )
        val product2 = productRepository.save(
            Product(name = "Product 2", price = BigDecimal("50.00"), stock = 5)
        )

        val request = CreateOrderRequest(
            items = listOf(
                OrderItemRequest(productId = product1.id, quantity = 2),
                OrderItemRequest(productId = product2.id, quantity = 1)
            )
        )

        mockMvc.perform(
            post("/api/order")
                .header("satoken", saToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.buyerId").value(testUserId))
            .andExpect(jsonPath("$.data.totalAmount").value(250.00))
            .andExpect(jsonPath("$.data.status").value(0))
            .andExpect(jsonPath("$.data.items.length()").value(2))
            .andExpect(jsonPath("$.data.orderNo").isNotEmpty)
    }

    @Test
    fun `should return error when product not found`() {
        val request = CreateOrderRequest(
            items = listOf(
                OrderItemRequest(productId = 99999L, quantity = 1)
            )
        )

        mockMvc.perform(
            post("/api/order")
                .header("satoken", saToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(ErrorCode.PRODUCT_NOT_FOUND))
    }

    @Test
    fun `should return error when stock not enough`() {
        val product = productRepository.save(
            Product(name = "Limited Product", price = BigDecimal("100.00"), stock = 1)
        )

        val request = CreateOrderRequest(
            items = listOf(
                OrderItemRequest(productId = product.id, quantity = 100)
            )
        )

        mockMvc.perform(
            post("/api/order")
                .header("satoken", saToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(ErrorCode.STOCK_NOT_ENOUGH))
    }

    @Test
    fun `should pay order successfully`() {
        // Create product and order first
        val product = productRepository.save(
            Product(name = "Product", price = BigDecimal("100.00"), stock = 10)
        )

        val createRequest = CreateOrderRequest(
            items = listOf(OrderItemRequest(productId = product.id, quantity = 1))
        )

        val createResult = mockMvc.perform(
            post("/api/order")
                .header("satoken", saToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isOk)
            .andReturn()

        val responseJson = objectMapper.readTree(createResult.response.contentAsString)
        val orderId = responseJson["data"]["id"].asLong()

        // Pay the order
        mockMvc.perform(
            put("/api/order/$orderId/pay")
                .header("satoken", saToken)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.status").value(1))
    }

    @Test
    fun `should cancel order and restore stock`() {
        // Create product and order
        val product = productRepository.save(
            Product(name = "Product", price = BigDecimal("100.00"), stock = 10)
        )

        val createRequest = CreateOrderRequest(
            items = listOf(OrderItemRequest(productId = product.id, quantity = 3))
        )

        val createResult = mockMvc.perform(
            post("/api/order")
                .header("satoken", saToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isOk)
            .andReturn()

        val responseJson = objectMapper.readTree(createResult.response.contentAsString)
        val orderId = responseJson["data"]["id"].asLong()

        // Verify stock was deducted
        val productAfterOrder = productRepository.findById(product.id).get()
        org.junit.jupiter.api.Assertions.assertEquals(7, productAfterOrder.stock)

        // Cancel the order
        mockMvc.perform(
            put("/api/order/$orderId/cancel")
                .header("satoken", saToken)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.status").value(2))

        // Verify stock was restored
        val productAfterCancel = productRepository.findById(product.id).get()
        org.junit.jupiter.api.Assertions.assertEquals(10, productAfterCancel.stock)
    }

    @Test
    fun `should return error when order not found`() {
        mockMvc.perform(
            get("/api/order/99999")
                .header("satoken", saToken)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(ErrorCode.ORDER_NOT_FOUND))
    }

    @Test
    fun `should return sold orders for seller with paid orders only`() {
        val product = productRepository.save(
            Product(name = "Seller Product", price = BigDecimal("88.00"), stock = 20, sellerId = testUserId)
        )

        val pendingOrderRequest = CreateOrderRequest(
            items = listOf(OrderItemRequest(productId = product.id, quantity = 1))
        )
        mockMvc.perform(
            post("/api/order")
                .header("satoken", buyerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pendingOrderRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))

        val paidOrderResult = mockMvc.perform(
            post("/api/order")
                .header("satoken", buyerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pendingOrderRequest))
        )
            .andExpect(status().isOk)
            .andReturn()
        val paidOrderId = objectMapper.readTree(paidOrderResult.response.contentAsString)["data"]["id"].asLong()

        mockMvc.perform(
            put("/api/order/$paidOrderId/pay")
                .header("satoken", buyerToken)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))

        mockMvc.perform(
            get("/api/order/sold")
                .header("satoken", saToken)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].status").value(1))
    }

    @Test
    fun `should request refund for paid order via API`() {
        val product = productRepository.save(
            Product(name = "Refund Product", price = BigDecimal("120.00"), stock = 10, sellerId = testUserId)
        )
        val createRequest = CreateOrderRequest(
            items = listOf(OrderItemRequest(productId = product.id, quantity = 1))
        )
        val createResult = mockMvc.perform(
            post("/api/order")
                .header("satoken", buyerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isOk)
            .andReturn()
        val orderId = objectMapper.readTree(createResult.response.contentAsString)["data"]["id"].asLong()

        mockMvc.perform(
            put("/api/order/$orderId/pay")
                .header("satoken", buyerToken)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))

        mockMvc.perform(
            put("/api/order/$orderId/refund-request")
                .header("satoken", buyerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"reason":"不想要了"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.status").value(3))
            .andExpect(jsonPath("$.data.refundReason").value("不想要了"))
    }

    @Test
    fun `should confirm refund by seller`() {
        val product = productRepository.save(
            Product(name = "Refund Confirm Product", price = BigDecimal("120.00"), stock = 10, sellerId = testUserId)
        )
        val createRequest = CreateOrderRequest(
            items = listOf(OrderItemRequest(productId = product.id, quantity = 1))
        )
        val createResult = mockMvc.perform(
            post("/api/order")
                .header("satoken", buyerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isOk)
            .andReturn()
        val orderId = objectMapper.readTree(createResult.response.contentAsString)["data"]["id"].asLong()

        mockMvc.perform(
            put("/api/order/$orderId/pay")
                .header("satoken", buyerToken)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))

        mockMvc.perform(
            put("/api/order/$orderId/refund-request")
                .header("satoken", buyerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"reason":"不想要了"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.status").value(3))

        mockMvc.perform(
            put("/api/order/$orderId/refund-confirm")
                .header("satoken", saToken)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.status").value(4))
    }

    @Test
    fun `should reject refund confirmation by non seller`() {
        val product = productRepository.save(
            Product(name = "Refund Confirm Deny Product", price = BigDecimal("88.00"), stock = 10, sellerId = testUserId)
        )
        val createRequest = CreateOrderRequest(
            items = listOf(OrderItemRequest(productId = product.id, quantity = 1))
        )
        val createResult = mockMvc.perform(
            post("/api/order")
                .header("satoken", buyerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isOk)
            .andReturn()
        val orderId = objectMapper.readTree(createResult.response.contentAsString)["data"]["id"].asLong()

        mockMvc.perform(
            put("/api/order/$orderId/pay")
                .header("satoken", buyerToken)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))

        mockMvc.perform(
            put("/api/order/$orderId/refund-request")
                .header("satoken", buyerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"reason":"不想要了"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.status").value(3))

        mockMvc.perform(
            put("/api/order/$orderId/refund-confirm")
                .header("satoken", buyerToken)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN))
    }

    @Test
    fun `should validate refund reason`() {
        val product = productRepository.save(
            Product(name = "Refund Validate Product", price = BigDecimal("66.00"), stock = 10, sellerId = testUserId)
        )
        val createRequest = CreateOrderRequest(
            items = listOf(OrderItemRequest(productId = product.id, quantity = 1))
        )
        val createResult = mockMvc.perform(
            post("/api/order")
                .header("satoken", buyerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isOk)
            .andReturn()
        val orderId = objectMapper.readTree(createResult.response.contentAsString)["data"]["id"].asLong()

        mockMvc.perform(
            put("/api/order/$orderId/pay")
                .header("satoken", buyerToken)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))

        mockMvc.perform(
            put("/api/order/$orderId/refund-request")
                .header("satoken", buyerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"reason":""}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_ERROR))
    }
}
