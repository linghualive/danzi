package com.live.commerce.base.service

import com.live.commerce.base.dto.SendPrivateMessageRequest
import com.live.commerce.base.dto.UpdateProfileRequest
import com.live.commerce.base.entity.PrivateMessage
import com.live.commerce.base.entity.User
import com.live.commerce.base.entity.UserFollow
import com.live.commerce.base.entity.UserProfile
import com.live.commerce.base.repository.PrivateMessageRepository
import com.live.commerce.base.repository.UserFollowRepository
import com.live.commerce.base.repository.UserProfileRepository
import com.live.commerce.base.repository.UserRepository
import com.live.commerce.base.service.impl.UserSocialServiceImpl
import com.live.commerce.common.exception.BusinessException
import com.live.commerce.common.exception.ErrorCode
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Optional

class UserSocialServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val userProfileRepository = mockk<UserProfileRepository>()
    private val userFollowRepository = mockk<UserFollowRepository>()
    private val privateMessageRepository = mockk<PrivateMessageRepository>()

    private lateinit var service: UserSocialServiceImpl

    @BeforeEach
    fun setUp() {
        service = UserSocialServiceImpl(
            userRepository,
            userProfileRepository,
            userFollowRepository,
            privateMessageRepository
        )
    }

    @Test
    fun `should update my profile successfully`() {
        val user = User(id = 1L, username = "u1", nickname = "old", role = 0)
        every { userRepository.findById(1L) } returns Optional.of(user)
        every { userProfileRepository.findByUserId(1L) } returns UserProfile(userId = 1L, bio = "old")
        every { userRepository.save(any()) } answers { firstArg() }
        every { userProfileRepository.save(any()) } answers { firstArg() }

        val result = service.updateMyProfile(
            1L,
            UpdateProfileRequest(nickname = "new", bio = "new-bio", avatarFileId = 12L)
        )

        assertEquals("new", result.nickname)
        assertEquals("new-bio", result.bio)
        assertTrue(result.avatarUrl?.contains("/api/base/media/public/12") == true)
    }

    @Test
    fun `should reject follow self`() {
        val ex = assertThrows<BusinessException> { service.follow(1L, 1L) }
        assertEquals(ErrorCode.CANNOT_FOLLOW_SELF, ex.code)
    }

    @Test
    fun `should reject duplicate follow`() {
        every { userRepository.findById(2L) } returns Optional.of(User(id = 2L, username = "u2", nickname = "u2"))
        every { userFollowRepository.existsByFollowerIdAndFolloweeId(1L, 2L) } returns true

        val ex = assertThrows<BusinessException> { service.follow(1L, 2L) }
        assertEquals(ErrorCode.USER_ALREADY_FOLLOWED, ex.code)
    }

    @Test
    fun `should send private message successfully`() {
        every { userRepository.findById(1L) } returns Optional.of(User(id = 1L, username = "u1", nickname = "用户1"))
        every { userRepository.findById(2L) } returns Optional.of(User(id = 2L, username = "u2", nickname = "用户2"))
        every { privateMessageRepository.save(any()) } answers {
            firstArg<PrivateMessage>().apply { id = 99L }
        }

        val result = service.sendMessage(1L, SendPrivateMessageRequest(receiverId = 2L, content = "hello"))

        assertEquals(99L, result.id)
        assertEquals(1L, result.senderId)
        assertEquals(2L, result.receiverId)
        assertEquals("hello", result.content)
    }

    @Test
    fun `should return follow stats`() {
        every { userFollowRepository.countByFollowerId(2L) } returns 7L
        every { userFollowRepository.countByFolloweeId(2L) } returns 3L
        every { userFollowRepository.existsByFollowerIdAndFolloweeId(1L, 2L) } returns true

        val stats = service.getFollowStats(1L, 2L)

        assertEquals(7L, stats.followingCount)
        assertEquals(3L, stats.followerCount)
        assertTrue(stats.followedByMe)
    }
}
