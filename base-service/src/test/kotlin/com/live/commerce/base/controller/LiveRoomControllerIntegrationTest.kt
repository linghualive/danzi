package com.live.commerce.base.controller

import cn.dev33.satoken.stp.StpUtil
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.live.commerce.base.TestcontainersConfig
import com.live.commerce.base.dto.CreateRoomRequest
import com.live.commerce.base.dto.LoginRequest
import com.live.commerce.base.dto.RegisterRequest
import com.live.commerce.base.entity.BroadcastQualification
import com.live.commerce.base.repository.BroadcastQualificationRepository
import com.live.commerce.common.exception.ErrorCode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
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
class LiveRoomControllerIntegrationTest : TestcontainersConfig() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var qualificationRepository: BroadcastQualificationRepository

    private val objectMapper = jacksonObjectMapper()
    private var token: String = ""
    private var userId: Long = 0

    @BeforeEach
    fun setUp() {
        // Register and login to get a token
        val username = "roomuser_${System.nanoTime()}"
        val registerReq = RegisterRequest(username, "password123", "Room User")
        mockMvc.perform(
            post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq))
        )

        val loginReq = LoginRequest(username, "password123")
        val loginResult = mockMvc.perform(
            post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq))
        ).andReturn()

        val loginJson = objectMapper.readTree(loginResult.response.contentAsString)
        token = loginJson["data"]["token"].asText()
        userId = loginJson["data"]["userId"].asLong()

        // Grant broadcast qualification
        qualificationRepository.save(BroadcastQualification(userId = userId, contactInfo = "test", status = 1))
    }

    @Test
    fun `should create room successfully`() {
        val request = CreateRoomRequest("My Live Room")
        mockMvc.perform(
            post("/api/live/room")
                .header("satoken", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.title").value("My Live Room"))
            .andExpect(jsonPath("$.data.streamKey").isNotEmpty)
            .andExpect(jsonPath("$.data.pushUrl").isNotEmpty)
    }

    @Test
    fun `should get room by id`() {
        // Create room first
        val request = CreateRoomRequest("Room to Get")
        val createResult = mockMvc.perform(
            post("/api/live/room")
                .header("satoken", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andReturn()

        val roomId = objectMapper.readTree(createResult.response.contentAsString)["data"]["id"].asLong()

        mockMvc.perform(
            get("/api/live/room/$roomId")
                .header("satoken", token)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.title").value("Room to Get"))
    }

    @Test
    fun `should list rooms with pagination`() {
        // Create multiple rooms
        repeat(3) {
            val request = CreateRoomRequest("Room $it")
            mockMvc.perform(
                post("/api/live/room")
                    .header("satoken", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
        }

        mockMvc.perform(
            get("/api/live/room/list")
                .header("satoken", token)
                .param("page", "0")
                .param("size", "10")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.content").isArray)
    }

    @Test
    fun `should start live successfully`() {
        val request = CreateRoomRequest("Room to Start")
        val createResult = mockMvc.perform(
            post("/api/live/room")
                .header("satoken", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andReturn()

        val roomId = objectMapper.readTree(createResult.response.contentAsString)["data"]["id"].asLong()

        mockMvc.perform(
            put("/api/live/room/$roomId/start")
                .header("satoken", token)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.status").value(1))
    }

    @Test
    fun `should stop live successfully`() {
        val request = CreateRoomRequest("Room to Stop")
        val createResult = mockMvc.perform(
            post("/api/live/room")
                .header("satoken", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andReturn()

        val roomId = objectMapper.readTree(createResult.response.contentAsString)["data"]["id"].asLong()

        // Start first
        mockMvc.perform(
            put("/api/live/room/$roomId/start")
                .header("satoken", token)
        ).andExpect(status().isOk)

        // Then stop
        mockMvc.perform(
            put("/api/live/room/$roomId/stop")
                .header("satoken", token)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.status").value(2))
    }

    @Test
    fun `should return error when room not found`() {
        mockMvc.perform(
            get("/api/live/room/99999")
                .header("satoken", token)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(ErrorCode.ROOM_NOT_FOUND))
    }
}
