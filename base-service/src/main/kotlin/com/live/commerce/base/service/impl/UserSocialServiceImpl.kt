package com.live.commerce.base.service.impl

import com.live.commerce.base.dto.*
import com.live.commerce.base.entity.PrivateMessage
import com.live.commerce.base.entity.UserFollow
import com.live.commerce.base.entity.UserProfile
import com.live.commerce.base.repository.PrivateMessageRepository
import com.live.commerce.base.repository.UserFollowRepository
import com.live.commerce.base.repository.UserProfileRepository
import com.live.commerce.base.repository.UserRepository
import com.live.commerce.base.service.UserSocialService
import com.live.commerce.common.exception.BusinessException
import com.live.commerce.common.exception.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class UserSocialServiceImpl(
    private val userRepository: UserRepository,
    private val userProfileRepository: UserProfileRepository,
    private val userFollowRepository: UserFollowRepository,
    private val privateMessageRepository: PrivateMessageRepository
) : UserSocialService {

    override fun getMyProfile(userId: Long): UserProfileDTO = getProfile(userId)

    override fun getProfile(userId: Long): UserProfileDTO {
        val user = userRepository.findById(userId)
            .orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
        val profile = userProfileRepository.findByUserId(userId)
        return toProfileDTO(user.id, user.username, user.nickname, user.role, profile)
    }

    @Transactional
    override fun updateMyProfile(userId: Long, request: UpdateProfileRequest): UserProfileDTO {
        val user = userRepository.findById(userId)
            .orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
        val profile = userProfileRepository.findByUserId(userId) ?: UserProfile(userId = userId)

        request.nickname?.trim()?.takeIf { it.isNotBlank() }?.let {
            user.nickname = it
        }
        request.bio?.let { profile.bio = it.trim() }
        if (request.avatarFileId != null) {
            profile.avatarFileId = request.avatarFileId
            user.avatar = "/api/base/media/public/${request.avatarFileId}"
        }

        profile.updatedAt = LocalDateTime.now()
        user.updatedAt = LocalDateTime.now()
        userRepository.save(user)
        userProfileRepository.save(profile)
        return toProfileDTO(user.id, user.username, user.nickname, user.role, profile)
    }

    @Transactional
    override fun follow(currentUserId: Long, targetId: Long) {
        if (currentUserId == targetId) {
            throw BusinessException(ErrorCode.CANNOT_FOLLOW_SELF)
        }
        userRepository.findById(targetId).orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }

        if (userFollowRepository.existsByFollowerIdAndFolloweeId(currentUserId, targetId)) {
            throw BusinessException(ErrorCode.USER_ALREADY_FOLLOWED)
        }

        userFollowRepository.save(
            UserFollow(
                followerId = currentUserId,
                followeeId = targetId
            )
        )
    }

    @Transactional
    override fun unfollow(currentUserId: Long, targetId: Long) {
        userFollowRepository.deleteByFollowerIdAndFolloweeId(currentUserId, targetId)
    }

    override fun getFollowStats(currentUserId: Long, targetId: Long): FollowStatsDTO {
        val followingCount = userFollowRepository.countByFollowerId(targetId)
        val followerCount = userFollowRepository.countByFolloweeId(targetId)
        val followedByMe = userFollowRepository.existsByFollowerIdAndFolloweeId(currentUserId, targetId)
        return FollowStatsDTO(
            userId = targetId,
            followingCount = followingCount,
            followerCount = followerCount,
            followedByMe = followedByMe
        )
    }

    @Transactional
    override fun sendMessage(senderId: Long, request: SendPrivateMessageRequest): PrivateMessageDTO {
        val receiverId = request.receiverId ?: throw BusinessException(ErrorCode.PARAM_ERROR)
        if (senderId == receiverId) {
            throw BusinessException(ErrorCode.PARAM_ERROR, "不能给自己发送私信")
        }

        val sender = userRepository.findById(senderId).orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
        val receiver = userRepository.findById(receiverId).orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }

        val saved = privateMessageRepository.save(
            PrivateMessage(
                senderId = senderId,
                receiverId = receiverId,
                content = request.content.trim()
            )
        )
        return PrivateMessageDTO(
            id = saved.id,
            senderId = senderId,
            senderName = sender.nickname,
            receiverId = receiverId,
            receiverName = receiver.nickname,
            content = saved.content,
            createdAt = saved.createdAt
        )
    }

    override fun getConversation(currentUserId: Long, targetId: Long): List<PrivateMessageDTO> {
        val users = userRepository.findAllById(listOf(currentUserId, targetId)).associateBy { it.id }
        val currentName = users[currentUserId]?.nickname ?: "未知用户"
        val targetName = users[targetId]?.nickname ?: "未知用户"
        return privateMessageRepository.findConversation(currentUserId, targetId).map { msg ->
            PrivateMessageDTO(
                id = msg.id,
                senderId = msg.senderId,
                senderName = if (msg.senderId == currentUserId) currentName else targetName,
                receiverId = msg.receiverId,
                receiverName = if (msg.receiverId == currentUserId) currentName else targetName,
                content = msg.content,
                createdAt = msg.createdAt
            )
        }
    }

    override fun getConversations(currentUserId: Long): List<ConversationDTO> {
        val latest = privateMessageRepository.findLatestConversations(currentUserId)
        val targetIds = latest.map {
            if (it.senderId == currentUserId) it.receiverId else it.senderId
        }.distinct()
        val users = userRepository.findAllById(targetIds).associateBy { it.id }

        return latest.map { msg ->
            val targetId = if (msg.senderId == currentUserId) msg.receiverId else msg.senderId
            val targetUser = users[targetId]
            ConversationDTO(
                targetUserId = targetId,
                targetUserName = targetUser?.username ?: "unknown",
                targetNickname = targetUser?.nickname ?: "未知用户",
                lastMessage = msg.content,
                lastMessageAt = msg.createdAt
            )
        }
    }

    private fun toProfileDTO(
        userId: Long,
        username: String,
        nickname: String,
        role: Int,
        profile: UserProfile?
    ): UserProfileDTO {
        return UserProfileDTO(
            userId = userId,
            username = username,
            nickname = nickname,
            role = role,
            bio = profile?.bio,
            avatarUrl = profile?.avatarFileId?.let { "/api/base/media/public/$it" }
        )
    }
}
