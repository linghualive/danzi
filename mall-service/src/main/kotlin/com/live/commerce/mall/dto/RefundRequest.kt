package com.live.commerce.mall.dto

import jakarta.validation.constraints.NotBlank

data class RefundRequest(
    @field:NotBlank(message = "退款理由不能为空")
    val reason: String = ""
)
