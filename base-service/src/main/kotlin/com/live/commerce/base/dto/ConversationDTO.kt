package com.live.commerce.base.dto

import java.time.LocalDateTime

data class ConversationDTO(
    val targetUserId: Long,
    val targetUserName: String,
    val targetNickname: String,
    val lastMessage: String,
    val lastMessageAt: LocalDateTime
)
