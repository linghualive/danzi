package com.live.commerce.mall.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

data class CreateProductRequest(
    @field:NotBlank(message = "商品名称不能为空")
    val name: String,

    val description: String? = null,

    @field:NotNull(message = "价格不能为空")
    @field:DecimalMin(value = "0.01", message = "价格必须大于0")
    val price: BigDecimal,

    @field:NotNull(message = "库存不能为空")
    @field:Min(value = 0, message = "库存不能为负数")
    val stock: Int,

    val roomId: Long? = null,

    val imageFileId: Long? = null
)
