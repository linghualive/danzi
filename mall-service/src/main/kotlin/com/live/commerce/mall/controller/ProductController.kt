package com.live.commerce.mall.controller

import com.live.commerce.common.dto.PageResult
import com.live.commerce.common.dto.Result
import com.live.commerce.mall.dto.CreateProductRequest
import com.live.commerce.mall.dto.ProductDTO
import com.live.commerce.mall.dto.UpdateProductRequest
import com.live.commerce.mall.service.ProductService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/product")
class ProductController(
    private val productService: ProductService
) {

    @PostMapping
    fun createProduct(@Valid @RequestBody request: CreateProductRequest): Result<ProductDTO> {
        val product = productService.createProduct(request)
        return Result.ok(product)
    }

    @GetMapping("/{id}")
    fun getProduct(@PathVariable id: Long): Result<ProductDTO> {
        val product = productService.getProduct(id)
        return Result.ok(product)
    }

    @PutMapping("/{id}")
    fun updateProduct(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateProductRequest
    ): Result<ProductDTO> {
        val product = productService.updateProduct(id, request)
        return Result.ok(product)
    }

    @DeleteMapping("/{id}")
    fun deleteProduct(@PathVariable id: Long): Result<Nothing> {
        productService.deleteProduct(id)
        return Result.ok()
    }

    @GetMapping("/list")
    fun listProducts(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) keyword: String?
    ): Result<PageResult<ProductDTO>> {
        val result = productService.listProducts(page, size, keyword)
        return Result.ok(result)
    }

    @GetMapping("/room/{roomId}")
    fun getProductsByRoomId(@PathVariable roomId: Long): Result<List<ProductDTO>> {
        val products = productService.getProductsByRoomId(roomId)
        return Result.ok(products)
    }
}
