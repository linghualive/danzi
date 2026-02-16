package com.live.commerce.mall.service

import com.live.commerce.common.exception.BusinessException
import com.live.commerce.common.exception.ErrorCode
import com.live.commerce.mall.dto.CreateProductRequest
import com.live.commerce.mall.dto.UpdateProductRequest
import com.live.commerce.mall.entity.Product
import com.live.commerce.mall.repository.ProductRepository
import com.live.commerce.mall.service.impl.ProductServiceImpl
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

class ProductServiceTest {

    private lateinit var productRepository: ProductRepository
    private lateinit var productService: ProductService

    @BeforeEach
    fun setUp() {
        productRepository = mockk()
        productService = ProductServiceImpl(productRepository)
    }

    @Test
    fun `should create product successfully`() {
        val request = CreateProductRequest(
            name = "Test Product",
            description = "A test product",
            price = BigDecimal("99.99"),
            stock = 100,
            roomId = 1L,
            image = "http://example.com/image.png"
        )

        every { productRepository.save(any()) } answers {
            val product = firstArg<Product>()
            product.apply { id = 1L }
        }

        val result = productService.createProduct(request)

        assertNotNull(result)
        assertEquals(1L, result.id)
        assertEquals("Test Product", result.name)
        assertEquals("A test product", result.description)
        assertEquals(BigDecimal("99.99"), result.price)
        assertEquals(100, result.stock)
        assertEquals(1L, result.roomId)
        verify { productRepository.save(any()) }
    }

    @Test
    fun `should throw exception when price is negative`() {
        val request = CreateProductRequest(
            name = "Test Product",
            description = "A test product",
            price = BigDecimal("-1.00"),
            stock = 100
        )

        val exception = assertThrows<BusinessException> {
            productService.createProduct(request)
        }
        assertEquals(ErrorCode.PARAM_ERROR, exception.code)
    }

    @Test
    fun `should throw exception when stock is negative`() {
        val request = CreateProductRequest(
            name = "Test Product",
            description = "A test product",
            price = BigDecimal("99.99"),
            stock = -1
        )

        val exception = assertThrows<BusinessException> {
            productService.createProduct(request)
        }
        assertEquals(ErrorCode.PARAM_ERROR, exception.code)
    }

    @Test
    fun `should find product by id`() {
        val product = Product(
            id = 1L,
            name = "Test Product",
            description = "A test product",
            price = BigDecimal("99.99"),
            stock = 100,
            roomId = 1L,
            image = "http://example.com/image.png",
            status = 1,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        every { productRepository.findById(1L) } returns Optional.of(product)

        val result = productService.getProduct(1L)

        assertNotNull(result)
        assertEquals(1L, result.id)
        assertEquals("Test Product", result.name)
    }

    @Test
    fun `should throw exception when product not found`() {
        every { productRepository.findById(999L) } returns Optional.empty()

        val exception = assertThrows<BusinessException> {
            productService.getProduct(999L)
        }
        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, exception.code)
    }

    @Test
    fun `should update product successfully`() {
        val existingProduct = Product(
            id = 1L,
            name = "Old Name",
            description = "Old description",
            price = BigDecimal("50.00"),
            stock = 50,
            roomId = 1L,
            status = 1,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val request = UpdateProductRequest(
            name = "New Name",
            description = "New description",
            price = BigDecimal("88.88"),
            stock = 200,
            image = "http://example.com/new-image.png",
            status = 0
        )

        every { productRepository.findById(1L) } returns Optional.of(existingProduct)
        every { productRepository.save(any()) } answers { firstArg() }

        val result = productService.updateProduct(1L, request)

        assertEquals("New Name", result.name)
        assertEquals("New description", result.description)
        assertEquals(BigDecimal("88.88"), result.price)
        assertEquals(200, result.stock)
        assertEquals(0, result.status)
        verify { productRepository.save(any()) }
    }

    @Test
    fun `should delete product successfully`() {
        val product = Product(
            id = 1L,
            name = "Test Product",
            price = BigDecimal("99.99"),
            stock = 100
        )

        every { productRepository.findById(1L) } returns Optional.of(product)
        every { productRepository.delete(product) } returns Unit

        productService.deleteProduct(1L)

        verify { productRepository.findById(1L) }
        verify { productRepository.delete(product) }
    }

    @Test
    fun `should list products with pagination`() {
        val products = listOf(
            Product(id = 1L, name = "Product 1", price = BigDecimal("10.00"), stock = 10),
            Product(id = 2L, name = "Product 2", price = BigDecimal("20.00"), stock = 20)
        )
        val pageable = PageRequest.of(0, 10)
        val page = PageImpl(products, pageable, 2L)

        every { productRepository.findAll(pageable) } returns page

        val result = productService.listProducts(0, 10, null)

        assertEquals(2, result.content.size)
        assertEquals(0, result.page)
        assertEquals(10, result.size)
        assertEquals(2L, result.totalElements)
    }

    @Test
    fun `should find products by room id`() {
        val products = listOf(
            Product(id = 1L, name = "Product 1", roomId = 1L, price = BigDecimal("10.00"), stock = 10),
            Product(id = 2L, name = "Product 2", roomId = 1L, price = BigDecimal("20.00"), stock = 20)
        )

        every { productRepository.findByRoomId(1L) } returns products

        val result = productService.getProductsByRoomId(1L)

        assertEquals(2, result.size)
        assertEquals(1L, result[0].roomId)
        assertEquals(1L, result[1].roomId)
    }

    @Test
    fun `should search products by keyword`() {
        val products = listOf(
            Product(id = 1L, name = "iPhone 15", price = BigDecimal("999.00"), stock = 50)
        )
        val pageable = PageRequest.of(0, 10)
        val page = PageImpl(products, pageable, 1L)

        every { productRepository.findByNameContaining("iPhone", pageable) } returns page

        val result = productService.listProducts(0, 10, "iPhone")

        assertEquals(1, result.content.size)
        assertEquals("iPhone 15", result.content[0].name)
    }

    @Test
    fun `should throw exception when updating non-existent product`() {
        val request = UpdateProductRequest(
            name = "New Name"
        )

        every { productRepository.findById(999L) } returns Optional.empty()

        val exception = assertThrows<BusinessException> {
            productService.updateProduct(999L, request)
        }
        assertEquals(ErrorCode.PRODUCT_NOT_FOUND, exception.code)
    }
}
