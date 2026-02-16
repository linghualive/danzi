package com.live.commerce.mall.dto

import java.math.BigDecimal

data class UpdateProductRequest(
    val name: String? = null,
    val description: String? = null,
    val price: BigDecimal? = null,
    val stock: Int? = null,
    val image: String? = null,
    val status: Int? = null
)
