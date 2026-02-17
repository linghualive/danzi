package com.live.commerce.base.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class SendPrivateMessageRequest(
    @field:NotNull(message = "接收用户不能为空")
    val receiverId: Long?,

    @field:NotBlank(message = "消息内容不能为空")
    val content: String = ""
)
