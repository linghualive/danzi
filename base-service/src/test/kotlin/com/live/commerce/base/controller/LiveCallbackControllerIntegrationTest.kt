package com.live.commerce.base.controller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.live.commerce.base.TestcontainersConfig
import com.live.commerce.base.dto.CreateRoomRequest
import com.live.commerce.base.dto.LoginRequest
import com.live.commerce.base.dto.RegisterRequest
import com.live.commerce.base.dto.SrsCallbackRequest
import com.live.commerce.base.entity.BroadcastQualification
import com.live.commerce.base.repository.BroadcastQualificationRepository
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
class LiveCallbackControllerIntegrationTest : TestcontainersConfig() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var qualificationRepository: BroadcastQualificationRepository

    private val objectMapper = jacksonObjectMapper()
    private var token: String = ""
    private var streamKey: String = ""

    @BeforeEach
    fun setUp() {
        // Register and login to get a token
        val username = "callbackuser_${System.nanoTime()}"
        val registerReq = RegisterRequest(username, "password123", "Callback User")
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
        val userId = loginJson["data"]["userId"].asLong()

        // Grant broadcast qualification
        qualificationRepository.save(BroadcastQualification(userId = userId, contactInfo = "test", status = 1))

        // Create a room to get a streamKey
        val createRoomReq = CreateRoomRequest("Callback Test Room")
        val createResult = mockMvc.perform(
            post("/api/live/room")
                .header("satoken", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRoomReq))
        ).andReturn()

        val roomJson = objectMapper.readTree(createResult.response.contentAsString)
        streamKey = roomJson["data"]["streamKey"].asText()
    }

    @Test
    fun `should accept publish callback and update room status`() {
        val request = SrsCallbackRequest(
            action = "on_publish",
            ip = "127.0.0.1",
            vhost = "__defaultVhost__",
            app = "live",
            stream = streamKey,
            param = ""
        )

        mockMvc.perform(
            post("/api/live/callback/on_publish")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(0))

        // Verify the room status was updated to 1 (live)
        mockMvc.perform(
            get("/api/live/room/list")
                .header("satoken", token)
                .param("page", "0")
                .param("size", "100")
        ).andExpect(status().isOk)
    }

    @Test
    fun `should accept unpublish callback and update room status`() {
        // First publish
        val publishRequest = SrsCallbackRequest(
            action = "on_publish",
            ip = "127.0.0.1",
            vhost = "__defaultVhost__",
            app = "live",
            stream = streamKey,
            param = ""
        )

        mockMvc.perform(
            post("/api/live/callback/on_publish")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(publishRequest))
        ).andExpect(status().isOk)

        // Then unpublish
        val unpublishRequest = SrsCallbackRequest(
            action = "on_unpublish",
            ip = "127.0.0.1",
            vhost = "__defaultVhost__",
            app = "live",
            stream = streamKey,
            param = ""
        )

        mockMvc.perform(
            post("/api/live/callback/on_unpublish")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(unpublishRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(0))
    }
}
