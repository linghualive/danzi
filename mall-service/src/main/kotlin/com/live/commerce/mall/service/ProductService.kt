package com.live.commerce.mall.service

import com.live.commerce.common.dto.PageResult
import com.live.commerce.mall.dto.CreateProductRequest
import com.live.commerce.mall.dto.ProductDTO
import com.live.commerce.mall.dto.UpdateProductRequest

interface ProductService {

    fun createProduct(operatorId: Long, request: CreateProductRequest): ProductDTO

    fun getProduct(id: Long): ProductDTO

    fun updateProduct(operatorId: Long, id: Long, request: UpdateProductRequest): ProductDTO

    fun deleteProduct(operatorId: Long, id: Long)

    fun listProducts(page: Int, size: Int, keyword: String?): PageResult<ProductDTO>

    fun getProductsByRoomId(roomId: Long): List<ProductDTO>
}
