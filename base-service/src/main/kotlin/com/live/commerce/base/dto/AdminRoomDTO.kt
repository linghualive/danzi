package com.live.commerce.base.dto

import java.time.LocalDateTime

data class AdminRoomDTO(
    val id: Long,
    val userId: Long,
    val title: String,
    val status: Int,
    val closedReason: String?,
    val ownerNickname: String? = null,
    val updatedAt: LocalDateTime
)
