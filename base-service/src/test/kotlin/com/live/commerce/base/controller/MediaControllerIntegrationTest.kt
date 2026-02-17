package com.live.commerce.base.controller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.live.commerce.base.TestcontainersConfig
import com.live.commerce.base.dto.LoginRequest
import com.live.commerce.base.dto.RegisterRequest
import com.live.commerce.common.exception.ErrorCode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MediaControllerIntegrationTest : TestcontainersConfig() {

    @Autowired
    lateinit var mockMvc: MockMvc

    private val objectMapper = jacksonObjectMapper()

    private fun registerAndLogin(usernamePrefix: String): String {
        val username = "${usernamePrefix}_${System.nanoTime()}"
        val registerReq = RegisterRequest(username, "password123", "昵称_$usernamePrefix")
        mockMvc.perform(
            post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))

        val loginReq = LoginRequest(username, "password123")
        val loginResult = mockMvc.perform(
            post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andReturn()

        val loginJson = objectMapper.readTree(loginResult.response.contentAsString)
        return loginJson["data"]["token"].asText()
    }

    @Test
    fun `should upload and fetch media file`() {
        val token = registerAndLogin("media")
        val file = MockMultipartFile("file", "avatar.png", "image/png", "png-content".toByteArray())

        val uploadResult = mockMvc.perform(
            multipart("/api/base/media/upload")
                .file(file)
                .header("satoken", token)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").isNumber)
            .andExpect(jsonPath("$.data.url").isNotEmpty)
            .andReturn()

        val mediaId = objectMapper.readTree(uploadResult.response.contentAsString)["data"]["id"].asLong()

        mockMvc.perform(get("/api/base/media/public/$mediaId"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "image/png"))
            .andExpect(content().bytes("png-content".toByteArray()))
    }

    @Test
    fun `should reject empty media file`() {
        val token = registerAndLogin("media_empty")
        val file = MockMultipartFile("file", "empty.png", "image/png", ByteArray(0))

        mockMvc.perform(
            multipart("/api/base/media/upload")
                .file(file)
                .header("satoken", token)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_ERROR))
    }
}
