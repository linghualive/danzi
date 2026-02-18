package com.live.commerce.base.dto

data class FollowUserDTO(
    val userId: Long,
    val username: String,
    val nickname: String,
    val avatarUrl: String?,
    val followedByMe: Boolean
)
