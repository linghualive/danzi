package com.live.commerce.mall.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "`order`")
class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(nullable = false, unique = true, length = 32)
    var orderNo: String = "",

    @Column(nullable = false)
    var userId: Long = 0,

    @Column(nullable = false)
    var sellerId: Long = 0,

    @Column(nullable = false, precision = 10, scale = 2)
    var totalAmount: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    var status: Int = 0, // 0-待支付 1-已支付 2-已取消 3-退款申请中 4-已退款

    @Column(nullable = false)
    var expireAt: LocalDateTime = LocalDateTime.now().plusMinutes(5),

    var paidAt: LocalDateTime? = null,

    var refundReason: String? = null,

    var refundRequestedAt: LocalDateTime? = null,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
