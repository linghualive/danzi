package com.live.commerce.mall.controller

import cn.dev33.satoken.stp.StpUtil
import com.fasterxml.jackson.databind.ObjectMapper
import com.live.commerce.common.exception.ErrorCode
import com.live.commerce.mall.TestcontainersConfig
import com.live.commerce.mall.dto.CreateProductRequest
import com.live.commerce.mall.dto.UpdateProductRequest
import com.live.commerce.mall.repository.ProductRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.math.BigDecimal

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerIntegrationTest : TestcontainersConfig() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var productRepository: ProductRepository

    private var token: String = ""

    @BeforeEach
    fun setUp() {
        productRepository.deleteAll()
        StpUtil.login(1L)
        token = StpUtil.getTokenValue()
    }

    @Test
    fun `should create product via API`() {
        val request = CreateProductRequest(
            name = "Test Product",
            description = "A test product",
            price = BigDecimal("99.99"),
            stock = 100,
            roomId = 1L
        )

        mockMvc.perform(
            post("/api/product")
                .header("satoken", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.name").value("Test Product"))
            .andExpect(jsonPath("$.data.price").value(99.99))
            .andExpect(jsonPath("$.data.stock").value(100))
            .andExpect(jsonPath("$.data.roomId").value(1))
    }

    @Test
    fun `should get product by id`() {
        // First create a product
        val request = CreateProductRequest(
            name = "Test Product",
            description = "A test product",
            price = BigDecimal("99.99"),
            stock = 100
        )

        val createResult = mockMvc.perform(
            post("/api/product")
                .header("satoken", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andReturn()

        val responseJson = objectMapper.readTree(createResult.response.contentAsString)
        val productId = responseJson["data"]["id"].asLong()

        // Then get the product
        mockMvc.perform(get("/api/product/$productId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").value(productId))
            .andExpect(jsonPath("$.data.name").value("Test Product"))
    }

    @Test
    fun `should return 404 when product not found`() {
        mockMvc.perform(get("/api/product/99999"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(ErrorCode.PRODUCT_NOT_FOUND))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `should update product`() {
        // First create a product
        val createRequest = CreateProductRequest(
            name = "Old Name",
            description = "Old description",
            price = BigDecimal("50.00"),
            stock = 50
        )

        val createResult = mockMvc.perform(
            post("/api/product")
                .header("satoken", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isOk)
            .andReturn()

        val responseJson = objectMapper.readTree(createResult.response.contentAsString)
        val productId = responseJson["data"]["id"].asLong()

        // Then update the product
        val updateRequest = UpdateProductRequest(
            name = "New Name",
            description = "New description",
            price = BigDecimal("88.88"),
            stock = 200
        )

        mockMvc.perform(
            put("/api/product/$productId")
                .header("satoken", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.name").value("New Name"))
            .andExpect(jsonPath("$.data.description").value("New description"))
            .andExpect(jsonPath("$.data.price").value(88.88))
            .andExpect(jsonPath("$.data.stock").value(200))
    }

    @Test
    fun `should delete product`() {
        // First create a product
        val createRequest = CreateProductRequest(
            name = "To Delete",
            price = BigDecimal("10.00"),
            stock = 10
        )

        val createResult = mockMvc.perform(
            post("/api/product")
                .header("satoken", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isOk)
            .andReturn()

        val responseJson = objectMapper.readTree(createResult.response.contentAsString)
        val productId = responseJson["data"]["id"].asLong()

        // Then delete the product
        mockMvc.perform(delete("/api/product/$productId").header("satoken", token))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))

        // Verify it's deleted
        mockMvc.perform(get("/api/product/$productId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(ErrorCode.PRODUCT_NOT_FOUND))
    }

    @Test
    fun `should list products with pagination`() {
        // Create multiple products
        for (i in 1..15) {
            val request = CreateProductRequest(
                name = "Product $i",
                price = BigDecimal("${i * 10}.00"),
                stock = i * 10
            )
            mockMvc.perform(
                post("/api/product")
                    .header("satoken", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
        }

        // Get first page
        mockMvc.perform(get("/api/product/list?page=0&size=10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.content.length()").value(10))
            .andExpect(jsonPath("$.data.totalElements").value(15))
            .andExpect(jsonPath("$.data.totalPages").value(2))
    }

    @Test
    fun `should search products by keyword`() {
        // Create products with different names
        val products = listOf(
            CreateProductRequest(name = "iPhone 15", price = BigDecimal("999.00"), stock = 50),
            CreateProductRequest(name = "iPhone 14", price = BigDecimal("899.00"), stock = 30),
            CreateProductRequest(name = "Samsung Galaxy", price = BigDecimal("799.00"), stock = 40)
        )

        products.forEach { request ->
            mockMvc.perform(
                post("/api/product")
                    .header("satoken", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
        }

        // Search by keyword
        mockMvc.perform(get("/api/product/list?page=0&size=10&keyword=iPhone"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.content.length()").value(2))
            .andExpect(jsonPath("$.data.totalElements").value(2))
    }

    @Test
    fun `should find products by room id`() {
        // Create products in different rooms
        val products = listOf(
            CreateProductRequest(name = "Room1 Product1", price = BigDecimal("10.00"), stock = 10, roomId = 1L),
            CreateProductRequest(name = "Room1 Product2", price = BigDecimal("20.00"), stock = 20, roomId = 1L),
            CreateProductRequest(name = "Room2 Product1", price = BigDecimal("30.00"), stock = 30, roomId = 2L)
        )

        products.forEach { request ->
            mockMvc.perform(
                post("/api/product")
                    .header("satoken", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
        }

        // Find by room ID 1
        mockMvc.perform(get("/api/product/room/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.length()").value(2))
    }
}
