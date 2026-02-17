package com.live.commerce.base.controller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.live.commerce.base.TestcontainersConfig
import com.live.commerce.base.dto.CreateRoomRequest
import com.live.commerce.base.dto.LoginRequest
import com.live.commerce.base.dto.RegisterRequest
import com.live.commerce.base.repository.RoomWarningRepository
import com.live.commerce.base.repository.UserRepository
import com.live.commerce.common.exception.ErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminControllerIntegrationTest : TestcontainersConfig() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var roomWarningRepository: RoomWarningRepository

    private val objectMapper = jacksonObjectMapper()

    private fun registerAndLogin(usernamePrefix: String): Pair<Long, String> {
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
        return Pair(loginJson["data"]["userId"].asLong(), loginJson["data"]["token"].asText())
    }

    @Test
    fun `should deny non admin access`() {
        val (_, normalToken) = registerAndLogin("admin_deny")

        mockMvc.perform(
            get("/api/admin/users")
                .header("satoken", normalToken)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN))
    }

    @Test
    fun `should list users and update role as admin`() {
        val (adminId, adminToken) = registerAndLogin("admin_user")
        val (targetUserId, _) = registerAndLogin("target_user")

        val admin = userRepository.findById(adminId).orElseThrow()
        admin.role = 2
        userRepository.save(admin)

        mockMvc.perform(
            get("/api/admin/users")
                .header("satoken", adminToken)
                .param("size", "20")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.content").isArray)

        mockMvc.perform(
            put("/api/admin/user/$targetUserId/role")
                .header("satoken", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"role":1}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))

        val target = userRepository.findById(targetUserId).orElseThrow()
        assertEquals(1, target.role)
    }

    @Test
    fun `should warn and close room as admin`() {
        val (adminId, adminToken) = registerAndLogin("admin_room")
        val (_, ownerToken) = registerAndLogin("room_owner")

        val admin = userRepository.findById(adminId).orElseThrow()
        admin.role = 2
        userRepository.save(admin)

        val createResult = mockMvc.perform(
            post("/api/live/room")
                .header("satoken", ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(CreateRoomRequest("被管理直播间")))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andReturn()
        val roomId = objectMapper.readTree(createResult.response.contentAsString)["data"]["id"].asLong()

        mockMvc.perform(
            post("/api/admin/room/$roomId/warn")
                .header("satoken", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"message":"请规范直播内容"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))

        val warnings = roomWarningRepository.findByRoomIdOrderByCreatedAtDesc(roomId)
        assertEquals(1, warnings.size)
        assertEquals("请规范直播内容", warnings.first().message)

        mockMvc.perform(
            put("/api/admin/room/$roomId/close")
                .header("satoken", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"reason":"违规"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.status").value(3))
            .andExpect(jsonPath("$.data.closedReason").value("违规"))

        mockMvc.perform(
            put("/api/live/room/$roomId/start")
                .header("satoken", ownerToken)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(ErrorCode.ROOM_CLOSED_BY_ADMIN))
    }
}
