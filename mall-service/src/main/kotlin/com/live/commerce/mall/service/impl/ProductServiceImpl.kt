package com.live.commerce.mall.service.impl

import com.live.commerce.common.dto.PageResult
import com.live.commerce.common.exception.BusinessException
import com.live.commerce.common.exception.ErrorCode
import com.live.commerce.mall.dto.CreateProductRequest
import com.live.commerce.mall.dto.ProductDTO
import com.live.commerce.mall.dto.UpdateProductRequest
import com.live.commerce.mall.entity.Product
import com.live.commerce.mall.repository.ProductRepository
import com.live.commerce.mall.service.ProductService
import com.live.commerce.mall.support.UserPermissionSupport
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ProductServiceImpl(
    private val productRepository: ProductRepository,
    private val userPermissionSupport: UserPermissionSupport
) : ProductService {

    override fun createProduct(operatorId: Long, request: CreateProductRequest): ProductDTO {
        userPermissionSupport.getUser(operatorId)

        if (request.price.signum() < 0) {
            throw BusinessException(ErrorCode.PARAM_ERROR, "价格不能为负数")
        }
        if (request.stock < 0) {
            throw BusinessException(ErrorCode.PARAM_ERROR, "库存不能为负数")
        }

        val product = Product(
            sellerId = operatorId,
            name = request.name,
            description = request.description,
            price = request.price,
            stock = request.stock,
            roomId = request.roomId,
            imageFileId = request.imageFileId
        )

        val savedProduct = productRepository.save(product)
        return ProductDTO.from(savedProduct)
    }

    override fun getProduct(id: Long): ProductDTO {
        val product = productRepository.findById(id)
            .orElseThrow { BusinessException(ErrorCode.PRODUCT_NOT_FOUND) }
        return ProductDTO.from(product)
    }

    override fun updateProduct(operatorId: Long, id: Long, request: UpdateProductRequest): ProductDTO {
        val product = productRepository.findById(id)
            .orElseThrow { BusinessException(ErrorCode.PRODUCT_NOT_FOUND) }
        userPermissionSupport.ensureProductOperator(operatorId, product.sellerId)

        request.name?.let { product.name = it }
        request.description?.let { product.description = it }
        request.price?.let { product.price = it }
        request.stock?.let { product.stock = it }
        request.imageFileId?.let { product.imageFileId = it }
        request.status?.let { product.status = it }
        product.updatedAt = LocalDateTime.now()

        val savedProduct = productRepository.save(product)
        return ProductDTO.from(savedProduct)
    }

    override fun deleteProduct(operatorId: Long, id: Long) {
        val product = productRepository.findById(id)
            .orElseThrow { BusinessException(ErrorCode.PRODUCT_NOT_FOUND) }
        userPermissionSupport.ensureProductOperator(operatorId, product.sellerId)
        productRepository.delete(product)
    }

    override fun listProducts(page: Int, size: Int, keyword: String?): PageResult<ProductDTO> {
        val pageable = PageRequest.of(page, size)

        val productPage = if (keyword.isNullOrBlank()) {
            productRepository.findAll(pageable)
        } else {
            productRepository.findByNameContaining(keyword, pageable)
        }

        return PageResult(
            content = productPage.content.map { ProductDTO.from(it) },
            page = productPage.number,
            size = productPage.size,
            totalElements = productPage.totalElements,
            totalPages = productPage.totalPages
        )
    }

    override fun getProductsByRoomId(roomId: Long): List<ProductDTO> {
        return productRepository.findByRoomId(roomId).map { ProductDTO.from(it) }
    }
}
