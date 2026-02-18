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
    fun `should return following list for user`() {
        val follows = listOf(
            UserFollow(id = 1L, followerId = 1L, followeeId = 2L),
            UserFollow(id = 2L, followerId = 1L, followeeId = 3L)
        )
        every { userFollowRepository.findByFollowerId(1L) } returns follows
        val users = listOf(
            User(id = 2L, username = "u2", nickname = "User2"),
            User(id = 3L, username = "u3", nickname = "User3")
        )
        every { userRepository.findAllById(listOf(2L, 3L)) } returns users
        every { userProfileRepository.findByUserId(2L) } returns UserProfile(userId = 2L, avatarFileId = 10L)
        every { userProfileRepository.findByUserId(3L) } returns null
        every { userFollowRepository.existsByFollowerIdAndFolloweeId(5L, 2L) } returns false
        every { userFollowRepository.existsByFollowerIdAndFolloweeId(5L, 3L) } returns true

        val result = service.getFollowingList(1L, 5L)

        assertEquals(2, result.size)
        assertEquals(2L, result[0].userId)
        assertEquals("User2", result[0].nickname)
        assertNotNull(result[0].avatarUrl)
        assertEquals(false, result[0].followedByMe)
        assertEquals(true, result[1].followedByMe)
    }

    @Test
    fun `should return follower list for user`() {
        val follows = listOf(
            UserFollow(id = 1L, followerId = 10L, followeeId = 1L),
            UserFollow(id = 2L, followerId = 11L, followeeId = 1L)
        )
        every { userFollowRepository.findByFolloweeId(1L) } returns follows
        val users = listOf(
            User(id = 10L, username = "u10", nickname = "Fan1"),
            User(id = 11L, username = "u11", nickname = "Fan2")
        )
        every { userRepository.findAllById(listOf(10L, 11L)) } returns users
        every { userProfileRepository.findByUserId(10L) } returns null
        every { userProfileRepository.findByUserId(11L) } returns null
        every { userFollowRepository.existsByFollowerIdAndFolloweeId(5L, 10L) } returns true
        every { userFollowRepository.existsByFollowerIdAndFolloweeId(5L, 11L) } returns false

        val result = service.getFollowerList(1L, 5L)

        assertEquals(2, result.size)
        assertEquals(10L, result[0].userId)
        assertEquals(true, result[0].followedByMe)
        assertEquals(false, result[1].followedByMe)
    }

    @Test
    fun `should indicate followedByMe status in following list`() {
        val follows = listOf(UserFollow(id = 1L, followerId = 1L, followeeId = 2L))
        every { userFollowRepository.findByFollowerId(1L) } returns follows
        every { userRepository.findAllById(listOf(2L)) } returns listOf(User(id = 2L, username = "u2", nickname = "N2"))
        every { userProfileRepository.findByUserId(2L) } returns null
        every { userFollowRepository.existsByFollowerIdAndFolloweeId(1L, 2L) } returns true

        val result = service.getFollowingList(1L, 1L)

        assertEquals(1, result.size)
        assertTrue(result[0].followedByMe)
    }

    @Test
    fun `should indicate followedByMe status in follower list`() {
        val follows = listOf(UserFollow(id = 1L, followerId = 2L, followeeId = 1L))
        every { userFollowRepository.findByFolloweeId(1L) } returns follows
        every { userRepository.findAllById(listOf(2L)) } returns listOf(User(id = 2L, username = "u2", nickname = "N2"))
        every { userProfileRepository.findByUserId(2L) } returns null
        every { userFollowRepository.existsByFollowerIdAndFolloweeId(1L, 2L) } returns false

        val result = service.getFollowerList(1L, 1L)

        assertEquals(1, result.size)
        assertEquals(false, result[0].followedByMe)
    }

    @Test
    fun `should return empty list when no followers`() {
        every { userFollowRepository.findByFolloweeId(99L) } returns emptyList()

        val result = service.getFollowerList(99L, 1L)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `should return empty list when not following anyone`() {
        every { userFollowRepository.findByFollowerId(99L) } returns emptyList()

        val result = service.getFollowingList(99L, 1L)

        assertTrue(result.isEmpty())
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
