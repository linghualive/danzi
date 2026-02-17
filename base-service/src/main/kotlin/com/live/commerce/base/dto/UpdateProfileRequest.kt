package com.live.commerce.base.dto

data class UpdateProfileRequest(
    val nickname: String? = null,
    val bio: String? = null,
    val avatarFileId: Long? = null
)
