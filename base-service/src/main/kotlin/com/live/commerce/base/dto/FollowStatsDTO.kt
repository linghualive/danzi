package com.live.commerce.base.dto

data class FollowStatsDTO(
    val userId: Long,
    val followingCount: Long,
    val followerCount: Long,
    val followedByMe: Boolean
)
