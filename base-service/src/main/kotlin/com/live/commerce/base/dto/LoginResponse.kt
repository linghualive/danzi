package com.live.commerce.base.dto

data class LoginResponse(
    val token: String,
    val userId: Long,
    val username: String,
    val nickname: String,
    val role: Int
)
