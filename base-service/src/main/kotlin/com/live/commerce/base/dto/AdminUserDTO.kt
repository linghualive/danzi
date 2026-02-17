package com.live.commerce.base.dto

import java.time.LocalDateTime

data class AdminUserDTO(
    val id: Long,
    val username: String,
    val nickname: String,
    val role: Int,
    val createdAt: LocalDateTime
)
