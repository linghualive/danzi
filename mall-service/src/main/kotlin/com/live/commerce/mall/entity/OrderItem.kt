package com.live.commerce.mall.entity

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "order_item")
class OrderItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(nullable = false)
    var orderId: Long = 0,

    @Column(nullable = false)
    var productId: Long = 0,

    @Column(nullable = false, length = 100)
    var productName: String = "",

    @Column(nullable = false, precision = 10, scale = 2)
    var price: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    var quantity: Int = 0
)
