package com.live.commerce.mall.dto

import com.live.commerce.mall.entity.Product
import java.math.BigDecimal
import java.time.LocalDateTime

data class ProductDTO(
    val id: Long,
    val roomId: Long?,
    val name: String,
    val description: String?,
    val price: BigDecimal,
    val stock: Int,
    val image: String?,
    val status: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(product: Product): ProductDTO = ProductDTO(
            id = product.id,
            roomId = product.roomId,
            name = product.name,
            description = product.description,
            price = product.price,
            stock = product.stock,
            image = product.image,
            status = product.status,
            createdAt = product.createdAt,
            updatedAt = product.updatedAt
        )
    }
}
