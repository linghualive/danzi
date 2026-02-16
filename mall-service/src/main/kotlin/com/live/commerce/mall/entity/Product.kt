package com.live.commerce.mall.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "product")
class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    var roomId: Long? = null,

    var name: String = "",

    var description: String? = null,

    var price: BigDecimal = BigDecimal.ZERO,

    var stock: Int = 0,

    var image: String? = null,

    var status: Int = 1, // 0-下架 1-上架

    var createdAt: LocalDateTime = LocalDateTime.now(),

    var updatedAt: LocalDateTime = LocalDateTime.now()
)
