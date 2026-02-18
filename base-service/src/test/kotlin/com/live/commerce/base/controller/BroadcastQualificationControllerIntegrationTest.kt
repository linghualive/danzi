package com.live.commerce.base.controller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.live.commerce.base.TestcontainersConfig
import com.live.commerce.base.dto.LoginRequest
import com.live.commerce.base.dto.RegisterRequest
import com.live.commerce.base.repository.BroadcastQualificationRepository
import com.live.commerce.base.repository.UserRepository
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
class BroadcastQualificationControllerIntegrationTest : TestcontainersConfig() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var qualificationRepository: BroadcastQualificationRepository

    private val objectMapper = jacksonObjectMapper()

    private fun registerAndLogin(usernamePrefix: String): Pair<Long, String> {
        val username = "${usernamePrefix}_${System.nanoTime()}"
        val registerReq = RegisterRequest(username, "password123", "昵称_$usernamePrefix")
        mockMvc.perform(
            post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq))
        ).andExpect(status().isOk).andExpect(jsonPath("$.code").value(200))

        val loginReq = LoginRequest(username, "password123")
        val loginResult = mockMvc.perform(
            post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq))
        ).andExpect(status().isOk).andExpect(jsonPath("$.code").value(200)).andReturn()

        val loginJson = objectMapper.readTree(loginResult.response.contentAsString)
        return Pair(loginJson["data"]["userId"].asLong(), loginJson["data"]["token"].asText())
    }

    @Test
    fun `should submit qualification successfully`() {
        val (_, token) = registerAndLogin("qual_submit")

        mockMvc.perform(
            post("/api/broadcast/qualification")
                .header("satoken", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"contactInfo":"13800138000","businessLicense":"BL123","personalInfo":"test info"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.contactInfo").value("13800138000"))
            .andExpect(jsonPath("$.data.status").value(0))
    }

    @Test
    fun `should reject duplicate submission`() {
        val (_, token) = registerAndLogin("qual_dup")

        mockMvc.perform(
            post("/api/broadcast/qualification")
                .header("satoken", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"contactInfo":"13800138000"}""")
        ).andExpect(status().isOk).andExpect(jsonPath("$.code").value(200))

        mockMvc.perform(
            post("/api/broadcast/qualification")
                .header("satoken", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"contactInfo":"13900139000"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(ErrorCode.QUALIFICATION_ALREADY_SUBMITTED))
    }

    @Test
    fun `should get my qualification`() {
        val (_, token) = registerAndLogin("qual_me")

        mockMvc.perform(
            post("/api/broadcast/qualification")
                .header("satoken", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"contactInfo":"13800138000"}""")
        ).andExpect(status().isOk)

        mockMvc.perform(
            get("/api/broadcast/qualification/me")
                .header("satoken", token)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.contactInfo").value("13800138000"))
    }

    @Test
    fun `should admin review qualification`() {
        val (adminId, adminToken) = registerAndLogin("qual_admin")
        val admin = userRepository.findById(adminId).orElseThrow()
        admin.role = 2
        userRepository.save(admin)

        val (_, userToken) = registerAndLogin("qual_user")

        mockMvc.perform(
            post("/api/broadcast/qualification")
                .header("satoken", userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"contactInfo":"13800138000","businessLicense":"BL999"}""")
        ).andExpect(status().isOk).andExpect(jsonPath("$.code").value(200))

        // Get the qualification ID
        val meResult = mockMvc.perform(
            get("/api/broadcast/qualification/me")
                .header("satoken", userToken)
        ).andExpect(status().isOk).andReturn()
        val qualId = objectMapper.readTree(meResult.response.contentAsString)["data"]["id"].asLong()

        // Admin approves
        mockMvc.perform(
            put("/api/admin/qualification/$qualId/review")
                .header("satoken", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":1}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.status").value(1))
    }

    @Test
    fun `should list pending qualifications for admin`() {
        val (adminId, adminToken) = registerAndLogin("qual_list_admin")
        val admin = userRepository.findById(adminId).orElseThrow()
        admin.role = 2
        userRepository.save(admin)

        mockMvc.perform(
            get("/api/admin/qualifications")
                .header("satoken", adminToken)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").isArray)
    }

    @Test
    fun `should check qualification status`() {
        val (_, token) = registerAndLogin("qual_check")

        mockMvc.perform(
            get("/api/broadcast/qualification/check")
                .header("satoken", token)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").value(false))
    }
}
