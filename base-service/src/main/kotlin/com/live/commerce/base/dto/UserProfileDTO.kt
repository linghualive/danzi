package com.live.commerce.base.dto

data class UserProfileDTO(
    val userId: Long,
    val username: String,
    val nickname: String,
    val role: Int,
    val bio: String?,
    val avatarUrl: String?
)
