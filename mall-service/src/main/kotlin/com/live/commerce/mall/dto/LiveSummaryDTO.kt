package com.live.commerce.mall.dto

import java.math.BigDecimal

data class LiveSummaryDTO(
    val totalOrders: Int,
    val totalAmount: BigDecimal,
    val items: List<LiveSummaryItemDTO>
)

data class LiveSummaryItemDTO(
    val productId: Long,
    val productName: String,
    val productImage: String?,
    val totalQuantity: Int,
    val totalAmount: BigDecimal
)
