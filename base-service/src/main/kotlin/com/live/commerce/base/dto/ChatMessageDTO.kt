package com.live.commerce.base.dto

import java.time.LocalDateTime

data class ChatMessageDTO(
    val id: Long,
    val roomId: Long,
    val userId: Long,
    val nickname: String,
    val type: String,
    val content: String,
    val createdAt: LocalDateTime
)
