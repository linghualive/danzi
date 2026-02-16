package com.live.commerce.mall

import cn.dev33.satoken.stp.StpUtil
import com.fasterxml.jackson.databind.ObjectMapper
import com.live.commerce.common.dto.Result
import com.live.commerce.common.dto.UserDTO
import com.live.commerce.common.exception.ErrorCode
import com.live.commerce.mall.dto.CreateOrderRequest
import com.live.commerce.mall.dto.OrderItemRequest
import com.live.commerce.mall.entity.Product
import com.live.commerce.mall.feign.UserFeignClient
import com.live.commerce.mall.repository.OrderItemRepository
import com.live.commerce.mall.repository.OrderRepository
import com.live.commerce.mall.repository.ProductRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
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
class E2EAndSecurityTest : TestcontainersConfig() {

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

    private val userIdA = 1L
    private val userIdB = 2L
    private var tokenA: String = ""
    private var tokenB: String = ""

    @BeforeEach
    fun setUp() {
        orderItemRepository.deleteAll()
        orderRepository.deleteAll()
        productRepository.deleteAll()

        // Login user A and get token
        StpUtil.login(userIdA)
        tokenA = StpUtil.getTokenValue()

        // Login user B and get token (Sa-Token supports multiple concurrent sessions)
        StpUtil.login(userIdB)
        tokenB = StpUtil.getTokenValue()

        // Mock Feign client for user A
        val userDTOA = UserDTO(id = userIdA, username = "testuser", nickname = "Test User", avatar = null, role = 0)
        Mockito.`when`(userFeignClient.getUserById(userIdA)).thenReturn(Result.ok(userDTOA))

        // Mock Feign client for user B
        val userDTOB = UserDTO(id = userIdB, username = "testuser2", nickname = "Test User 2", avatar = null, role = 0)
        Mockito.`when`(userFeignClient.getUserById(userIdB)).thenReturn(Result.ok(userDTOB))
    }

    @AfterEach
    fun tearDown() {
        try { StpUtil.logout(userIdA) } catch (_: Exception) {}
        try { StpUtil.logout(userIdB) } catch (_: Exception) {}
    }

    @Test
    fun `should complete shopping journey - browse products and create order and pay`() {
        // Create a product with price=100, stock=10
        val product = productRepository.save(
            Product(name = "Live Product", price = BigDecimal("100.00"), stock = 10)
        )

        // Create order with quantity=2
        val createRequest = CreateOrderRequest(
            items = listOf(OrderItemRequest(productId = product.id, quantity = 2))
        )

        val createResult = mockMvc.perform(
            post("/api/order")
                .header("satoken", tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.status").value(0))
            .andExpect(jsonPath("$.data.totalAmount").value(200.00))
            .andReturn()

        val responseJson = objectMapper.readTree(createResult.response.contentAsString)
        val orderId = responseJson["data"]["id"].asLong()

        // Verify stock was deducted
        val productAfterOrder = productRepository.findById(product.id).get()
        assertEquals(8, productAfterOrder.stock)

        // Pay the order
        mockMvc.perform(
            put("/api/order/$orderId/pay")
                .header("satoken", tokenA)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.status").value(1))

        // Verify stock is still 8 after payment (no change on pay)
        val productAfterPay = productRepository.findById(product.id).get()
        assertEquals(8, productAfterPay.stock)
    }

    @Test
    fun `should complete cancel journey - create order then cancel and verify stock restored`() {
        // Create product with stock=10
        val product = productRepository.save(
            Product(name = "Cancel Test Product", price = BigDecimal("50.00"), stock = 10)
        )

        // Create order with quantity=3
        val createRequest = CreateOrderRequest(
            items = listOf(OrderItemRequest(productId = product.id, quantity = 3))
        )

        val createResult = mockMvc.perform(
            post("/api/order")
                .header("satoken", tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andReturn()

        val responseJson = objectMapper.readTree(createResult.response.contentAsString)
        val orderId = responseJson["data"]["id"].asLong()

        // Verify stock was deducted to 7
        val productAfterOrder = productRepository.findById(product.id).get()
        assertEquals(7, productAfterOrder.stock)

        // Cancel the order
        mockMvc.perform(
            put("/api/order/$orderId/cancel")
                .header("satoken", tokenA)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.status").value(2))

        // Verify stock was restored to 10
        val productAfterCancel = productRepository.findById(product.id).get()
        assertEquals(10, productAfterCancel.stock)
    }

    @Test
    fun `should deny user from accessing another user order`() {
        // User A creates a product and order
        val product = productRepository.save(
            Product(name = "Security Test Product", price = BigDecimal("100.00"), stock = 10)
        )

        val createRequest = CreateOrderRequest(
            items = listOf(OrderItemRequest(productId = product.id, quantity = 1))
        )

        val createResult = mockMvc.perform(
            post("/api/order")
                .header("satoken", tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andReturn()

        val responseJson = objectMapper.readTree(createResult.response.contentAsString)
        val orderId = responseJson["data"]["id"].asLong()

        // User B tries to access User A's order
        mockMvc.perform(
            get("/api/order/$orderId")
                .header("satoken", tokenB)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN))
    }

    @Test
    fun `should verify cross-service feign call during order creation`() {
        // Create a product
        val product = productRepository.save(
            Product(name = "Feign Test Product", price = BigDecimal("100.00"), stock = 10)
        )

        val createRequest = CreateOrderRequest(
            items = listOf(OrderItemRequest(productId = product.id, quantity = 1))
        )

        // Create order
        mockMvc.perform(
            post("/api/order")
                .header("satoken", tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))

        // Verify Feign client was called exactly once with the correct userId
        Mockito.verify(userFeignClient, Mockito.times(1)).getUserById(userIdA)
    }

    @Test
    fun `should reject order creation when feign returns user not found`() {
        // Override mock to return user not found for a specific userId
        val nonExistentUserId = 999L
        StpUtil.login(nonExistentUserId)
        val tokenNonExistent = StpUtil.getTokenValue()

        Mockito.`when`(userFeignClient.getUserById(nonExistentUserId))
            .thenReturn(Result.error(ErrorCode.USER_NOT_FOUND, "用户不存在"))

        val product = productRepository.save(
            Product(name = "Feign Error Product", price = BigDecimal("100.00"), stock = 10)
        )

        val createRequest = CreateOrderRequest(
            items = listOf(OrderItemRequest(productId = product.id, quantity = 1))
        )

        // Try to create order with non-existent user
        mockMvc.perform(
            post("/api/order")
                .header("satoken", tokenNonExistent)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(ErrorCode.USER_NOT_FOUND))

        StpUtil.logout()
    }
}
