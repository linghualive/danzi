package com.live.commerce.common.dto

data class UserDTO(
    val id: Long,
    val username: String,
    val nickname: String,
    val avatar: String?,
    val role: Int
)
