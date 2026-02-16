package com.live.commerce.base.controller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.live.commerce.base.TestcontainersConfig
import com.live.commerce.base.dto.LoginRequest
import com.live.commerce.base.dto.RegisterRequest
import com.live.commerce.common.exception.ErrorCode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIntegrationTest : TestcontainersConfig() {

    @Autowired
    lateinit var mockMvc: MockMvc

    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `should register user successfully`() {
        val request = RegisterRequest("integuser1", "password123", "Integration User")
        mockMvc.perform(
            post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.username").value("integuser1"))
    }

    @Test
    fun `should fail when registering duplicate username`() {
        val request = RegisterRequest("dupuser", "password123", "Dup User")
        // First registration
        mockMvc.perform(
            post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isOk)

        // Duplicate registration
        mockMvc.perform(
            post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(ErrorCode.USERNAME_ALREADY_EXISTS))
    }

    @Test
    fun `should fail when required fields are missing`() {
        val request = mapOf("username" to "", "password" to "", "nickname" to "")
        mockMvc.perform(
            post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_ERROR))
    }

    @Test
    fun `should login successfully`() {
        // Register first
        val registerReq = RegisterRequest("loginuser", "password123", "Login User")
        mockMvc.perform(
            post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq))
        ).andExpect(status().isOk)

        // Login
        val loginReq = LoginRequest("loginuser", "password123")
        mockMvc.perform(
            post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.token").isNotEmpty)
            .andExpect(jsonPath("$.data.userId").isNumber)
    }

    @Test
    fun `should fail login with wrong password`() {
        val registerReq = RegisterRequest("wrongpwduser", "password123", "Wrong Pwd User")
        mockMvc.perform(
            post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq))
        ).andExpect(status().isOk)

        val loginReq = LoginRequest("wrongpwduser", "wrongpassword")
        mockMvc.perform(
            post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(ErrorCode.PASSWORD_INCORRECT))
    }

    @Test
    fun `should get user by id after login`() {
        // Register and login
        val registerReq = RegisterRequest("getusertest", "password123", "Get User")
        mockMvc.perform(
            post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq))
        ).andExpect(status().isOk)

        val loginReq = LoginRequest("getusertest", "password123")
        val loginResult = mockMvc.perform(
            post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq))
        ).andReturn()

        val loginJson = objectMapper.readTree(loginResult.response.contentAsString)
        val token = loginJson["data"]["token"].asText()
        val userId = loginJson["data"]["userId"].asLong()

        // Get user
        mockMvc.perform(
            get("/api/user/$userId")
                .header("satoken", token)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.username").value("getusertest"))
    }

    @Test
    fun `should return error when user not found`() {
        // Register and login first to get a token
        val registerReq = RegisterRequest("tokenholder", "password123", "Token Holder")
        mockMvc.perform(
            post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq))
        ).andExpect(status().isOk)

        val loginReq = LoginRequest("tokenholder", "password123")
        val loginResult = mockMvc.perform(
            post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq))
        ).andReturn()

        val loginJson = objectMapper.readTree(loginResult.response.contentAsString)
        val token = loginJson["data"]["token"].asText()

        mockMvc.perform(
            get("/api/user/99999")
                .header("satoken", token)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(ErrorCode.USER_NOT_FOUND))
    }
}
