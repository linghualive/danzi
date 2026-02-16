package com.live.commerce.base.service

import cn.dev33.satoken.stp.StpUtil
import com.live.commerce.base.dto.LoginRequest
import com.live.commerce.base.dto.RegisterRequest
import com.live.commerce.base.entity.User
import com.live.commerce.base.repository.UserRepository
import com.live.commerce.base.service.impl.UserServiceImpl
import com.live.commerce.common.exception.BusinessException
import com.live.commerce.common.exception.ErrorCode
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

class UserServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val passwordEncoder = BCryptPasswordEncoder()
    private lateinit var userService: UserServiceImpl

    @BeforeEach
    fun setUp() {
        userService = UserServiceImpl(userRepository, passwordEncoder)
        mockkStatic(StpUtil::class)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(StpUtil::class)
    }

    @Test
    fun `should register user with encrypted password`() {
        val request = RegisterRequest("testuser", "password123", "Test User")
        every { userRepository.findByUsername("testuser") } returns null
        val userSlot = slot<User>()
        every { userRepository.save(capture(userSlot)) } answers { userSlot.captured.apply { id = 1L } }

        val result = userService.register(request)

        assertEquals(1L, result.id)
        assertEquals("testuser", result.username)
        assertTrue(passwordEncoder.matches("password123", userSlot.captured.password))
    }

    @Test
    fun `should throw exception when username already exists`() {
        val request = RegisterRequest("testuser", "password123", "Test User")
        every { userRepository.findByUsername("testuser") } returns User(id = 1L, username = "testuser", password = "xxx", nickname = "Old")

        val exception = assertThrows<BusinessException> { userService.register(request) }
        assertEquals(ErrorCode.USERNAME_ALREADY_EXISTS, exception.code)
    }

    @Test
    fun `should login successfully and return token`() {
        val encodedPassword = passwordEncoder.encode("password123")
        val user = User(id = 1L, username = "testuser", password = encodedPassword, nickname = "Test User", role = 0)
        every { userRepository.findByUsername("testuser") } returns user
        every { StpUtil.login(any()) } just Runs
        every { StpUtil.getTokenValue() } returns "mock-token-12345"

        val response = userService.login(LoginRequest("testuser", "password123"))

        assertEquals(1L, response.userId)
        assertEquals("testuser", response.username)
        assertEquals("mock-token-12345", response.token)
    }

    @Test
    fun `should throw exception when username not found on login`() {
        every { userRepository.findByUsername("nonexistent") } returns null

        val exception = assertThrows<BusinessException> { userService.login(LoginRequest("nonexistent", "password123")) }
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.code)
    }

    @Test
    fun `should throw exception when password is incorrect`() {
        val encodedPassword = passwordEncoder.encode("correctpassword")
        val user = User(id = 1L, username = "testuser", password = encodedPassword, nickname = "Test User")
        every { userRepository.findByUsername("testuser") } returns user

        val exception = assertThrows<BusinessException> { userService.login(LoginRequest("testuser", "wrongpassword")) }
        assertEquals(ErrorCode.PASSWORD_INCORRECT, exception.code)
    }

    @Test
    fun `should find user by id`() {
        val user = User(id = 1L, username = "testuser", password = "xxx", nickname = "Test User", role = 1)
        every { userRepository.findById(1L) } returns java.util.Optional.of(user)

        val dto = userService.getUserById(1L)

        assertEquals(1L, dto.id)
        assertEquals("testuser", dto.username)
        assertEquals("Test User", dto.nickname)
        assertEquals(1, dto.role)
    }

    @Test
    fun `should throw exception when user not found by id`() {
        every { userRepository.findById(99L) } returns java.util.Optional.empty()

        val exception = assertThrows<BusinessException> { userService.getUserById(99L) }
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.code)
    }
}
