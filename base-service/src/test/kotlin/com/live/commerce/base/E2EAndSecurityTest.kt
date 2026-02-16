package com.live.commerce.base

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.live.commerce.base.dto.CreateRoomRequest
import com.live.commerce.base.dto.LoginRequest
import com.live.commerce.base.dto.RegisterRequest
import com.live.commerce.common.exception.ErrorCode
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
class E2EAndSecurityTest : TestcontainersConfig() {

    @Autowired
    lateinit var mockMvc: MockMvc

    private val objectMapper = jacksonObjectMapper()

    private fun registerAndLogin(username: String): Pair<Long, String> {
        val registerReq = RegisterRequest(username, "password123", "Test $username")
        mockMvc.perform(
            post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))

        val loginReq = LoginRequest(username, "password123")
        val loginResult = mockMvc.perform(
            post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq))
        ).andReturn()

        val loginJson = objectMapper.readTree(loginResult.response.contentAsString)
        val token = loginJson["data"]["token"].asText()
        val userId = loginJson["data"]["userId"].asLong()
        return Pair(userId, token)
    }

    @Test
    fun `should complete user registration and login journey`() {
        val username = "e2e_user_${System.nanoTime()}"

        // Step 1: Register
        val registerReq = RegisterRequest(username, "password123", "E2E User")
        val registerResult = mockMvc.perform(
            post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.username").value(username))
            .andExpect(jsonPath("$.data.nickname").value("E2E User"))
            .andReturn()

        val registerJson = objectMapper.readTree(registerResult.response.contentAsString)
        val userId = registerJson["data"]["id"].asLong()

        // Step 2: Login
        val loginReq = LoginRequest(username, "password123")
        val loginResult = mockMvc.perform(
            post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.token").isNotEmpty)
            .andExpect(jsonPath("$.data.userId").value(userId))
            .andReturn()

        val loginJson = objectMapper.readTree(loginResult.response.contentAsString)
        val token = loginJson["data"]["token"].asText()

        // Step 3: Get user info with token
        mockMvc.perform(
            get("/api/user/$userId")
                .header("satoken", token)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").value(userId))
            .andExpect(jsonPath("$.data.username").value(username))
            .andExpect(jsonPath("$.data.nickname").value("E2E User"))
    }

    @Test
    fun `should complete anchor live room journey`() {
        val username = "e2e_anchor_${System.nanoTime()}"
        val (_, token) = registerAndLogin(username)

        // Step 1: Create live room
        val createReq = CreateRoomRequest("E2E Live Room", "cover.jpg")
        val createResult = mockMvc.perform(
            post("/api/live/room")
                .header("satoken", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.title").value("E2E Live Room"))
            .andExpect(jsonPath("$.data.streamKey").isNotEmpty)
            .andExpect(jsonPath("$.data.status").value(0))
            .andReturn()

        val createJson = objectMapper.readTree(createResult.response.contentAsString)
        val roomId = createJson["data"]["id"].asLong()

        // Step 2: Start live
        mockMvc.perform(
            put("/api/live/room/$roomId/start")
                .header("satoken", token)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.status").value(1))

        // Step 3: Stop live
        mockMvc.perform(
            put("/api/live/room/$roomId/stop")
                .header("satoken", token)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.status").value(2))
    }

    @Test
    fun `should not leak password in API response`() {
        val username = "e2e_noleak_${System.nanoTime()}"

        // Step 1: Register and check response does not contain "password"
        val registerReq = RegisterRequest(username, "password123", "No Leak User")
        val registerResult = mockMvc.perform(
            post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andReturn()

        val registerBody = registerResult.response.contentAsString
        val registerJson = objectMapper.readTree(registerBody)
        val registerData = registerJson["data"]
        assert(!registerData.has("password")) { "Register response should not contain password field" }

        // Step 2: Login to get token and userId
        val loginReq = LoginRequest(username, "password123")
        val loginResult = mockMvc.perform(
            post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq))
        ).andReturn()

        val loginJson = objectMapper.readTree(loginResult.response.contentAsString)
        val token = loginJson["data"]["token"].asText()
        val userId = loginJson["data"]["userId"].asLong()

        // Step 3: Get user by id and check response does not contain "password"
        val getUserResult = mockMvc.perform(
            get("/api/user/$userId")
                .header("satoken", token)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andReturn()

        val getUserBody = getUserResult.response.contentAsString
        val getUserJson = objectMapper.readTree(getUserBody)
        val getUserData = getUserJson["data"]
        assert(!getUserData.has("password")) { "GetUser response should not contain password field" }
    }

    @Test
    fun `should deny non-owner from starting or stopping live`() {
        val usernameA = "e2e_ownerA_${System.nanoTime()}"
        val usernameB = "e2e_otherB_${System.nanoTime()}"

        // Register user A and create a live room
        val (_, tokenA) = registerAndLogin(usernameA)
        val createReq = CreateRoomRequest("Owner A Room")
        val createResult = mockMvc.perform(
            post("/api/live/room")
                .header("satoken", tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq))
        ).andReturn()

        val roomId = objectMapper.readTree(createResult.response.contentAsString)["data"]["id"].asLong()

        // Register user B
        val (_, tokenB) = registerAndLogin(usernameB)

        // User B tries to start User A's room -> should fail with ROOM_PERMISSION_DENIED
        mockMvc.perform(
            put("/api/live/room/$roomId/start")
                .header("satoken", tokenB)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(ErrorCode.ROOM_PERMISSION_DENIED))

        // Owner A starts the room so we can test stop permission
        mockMvc.perform(
            put("/api/live/room/$roomId/start")
                .header("satoken", tokenA)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))

        // User B tries to stop User A's room -> should fail with ROOM_PERMISSION_DENIED
        mockMvc.perform(
            put("/api/live/room/$roomId/stop")
                .header("satoken", tokenB)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(ErrorCode.ROOM_PERMISSION_DENIED))
    }

    @Test
    fun `should reject request with expired or invalid token`() {
        // Try to create a room with an invalid token
        // StpUtil.getLoginIdAsLong() will throw NotLoginException
        // which is caught by the generic Exception handler returning code 500
        val createReq = CreateRoomRequest("Invalid Token Room")
        mockMvc.perform(
            post("/api/live/room")
                .header("satoken", "invalid-token-xxx")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(500))

        // Also try without any token at all
        mockMvc.perform(
            post("/api/live/room")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(500))
    }
}
