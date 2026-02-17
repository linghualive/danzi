package com.live.commerce.base.dto

import jakarta.validation.constraints.NotBlank

data class AdminWarnRoomRequest(
    @field:NotBlank(message = "警告内容不能为空")
    val message: String = ""
)
