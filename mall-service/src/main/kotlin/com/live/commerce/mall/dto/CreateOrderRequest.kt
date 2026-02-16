package com.live.commerce.mall.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

data class CreateOrderRequest(
    @field:NotEmpty(message = "订单项不能为空")
    @field:Valid
    val items: List<OrderItemRequest>
)

data class OrderItemRequest(
    @field:NotNull(message = "商品ID不能为空")
    val productId: Long,

    @field:NotNull(message = "数量不能为空")
    @field:Min(value = 1, message = "数量必须大于0")
    val quantity: Int
)
