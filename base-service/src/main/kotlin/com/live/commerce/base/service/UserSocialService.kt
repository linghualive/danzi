package com.live.commerce.base.service

import com.live.commerce.base.dto.*

interface UserSocialService {
    fun getMyProfile(userId: Long): UserProfileDTO
    fun getProfile(userId: Long): UserProfileDTO
    fun updateMyProfile(userId: Long, request: UpdateProfileRequest): UserProfileDTO

    fun follow(currentUserId: Long, targetId: Long)
    fun unfollow(currentUserId: Long, targetId: Long)
    fun getFollowStats(currentUserId: Long, targetId: Long): FollowStatsDTO
    fun getFollowingList(targetId: Long, currentUserId: Long): List<FollowUserDTO>
    fun getFollowerList(targetId: Long, currentUserId: Long): List<FollowUserDTO>

    fun sendMessage(senderId: Long, request: SendPrivateMessageRequest): PrivateMessageDTO
    fun getConversation(currentUserId: Long, targetId: Long): List<PrivateMessageDTO>
    fun getConversations(currentUserId: Long): List<ConversationDTO>
}
