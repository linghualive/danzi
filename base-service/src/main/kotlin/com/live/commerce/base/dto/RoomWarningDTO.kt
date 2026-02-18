package com.live.commerce.base.dto

import java.time.LocalDateTime

data class RoomWarningDTO(
    val id: Long,
    val roomId: Long,
    val adminId: Long,
    val message: String,
    val createdAt: LocalDateTime
)
