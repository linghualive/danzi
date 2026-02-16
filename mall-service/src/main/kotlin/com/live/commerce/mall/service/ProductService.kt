package com.live.commerce.mall.service

import com.live.commerce.common.dto.PageResult
import com.live.commerce.mall.dto.CreateProductRequest
import com.live.commerce.mall.dto.ProductDTO
import com.live.commerce.mall.dto.UpdateProductRequest

interface ProductService {

    fun createProduct(request: CreateProductRequest): ProductDTO

    fun getProduct(id: Long): ProductDTO

    fun updateProduct(id: Long, request: UpdateProductRequest): ProductDTO

    fun deleteProduct(id: Long)

    fun listProducts(page: Int, size: Int, keyword: String?): PageResult<ProductDTO>

    fun getProductsByRoomId(roomId: Long): List<ProductDTO>
}
