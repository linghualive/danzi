package com.live.commerce.base.dto

import java.time.LocalDateTime

data class PrivateMessageDTO(
    val id: Long,
    val senderId: Long,
    val senderName: String,
    val receiverId: Long,
    val receiverName: String,
    val content: String,
    val createdAt: LocalDateTime
)
