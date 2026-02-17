package com.live.commerce.base.dto

import jakarta.validation.constraints.NotBlank

data class AdminCloseRoomRequest(
    @field:NotBlank(message = "关闭原因不能为空")
    val reason: String = ""
)
