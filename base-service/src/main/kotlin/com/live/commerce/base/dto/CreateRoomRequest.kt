package com.live.commerce.base.dto

import jakarta.validation.constraints.NotBlank

data class CreateRoomRequest(
    @field:NotBlank(message = "直播间标题不能为空")
    val title: String = "",
    val cover: String? = null
)
