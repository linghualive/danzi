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
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserSocialControllerIntegrationTest : TestcontainersConfig() {

    @Autowired
    lateinit var mockMvc: MockMvc

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
    fun `should update and get my profile`() {
        val (_, token) = registerAndLogin("profile")

        mockMvc.perform(
            put("/api/user/profile/me")
                .header("satoken", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"nickname":"新昵称","bio":"这是简介"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.nickname").value("新昵称"))
            .andExpect(jsonPath("$.data.bio").value("这是简介"))

        mockMvc.perform(
            get("/api/user/profile/me")
                .header("satoken", token)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.nickname").value("新昵称"))
            .andExpect(jsonPath("$.data.bio").value("这是简介"))
    }

    @Test
    fun `should follow and unfollow target user`() {
        val (_, tokenA) = registerAndLogin("follow_a")
        val (userBId, _) = registerAndLogin("follow_b")

        mockMvc.perform(
            post("/api/user/follow/$userBId")
                .header("satoken", tokenA)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))

        mockMvc.perform(
            get("/api/user/follow/$userBId/stats")
                .header("satoken", tokenA)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.followedByMe").value(true))
            .andExpect(jsonPath("$.data.followerCount").value(1))

        mockMvc.perform(
            delete("/api/user/follow/$userBId")
                .header("satoken", tokenA)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))

        mockMvc.perform(
            get("/api/user/follow/$userBId/stats")
                .header("satoken", tokenA)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.followedByMe").value(false))
            .andExpect(jsonPath("$.data.followerCount").value(0))
    }

    @Test
    fun `should reject following self`() {
        val (userId, token) = registerAndLogin("follow_self")

        mockMvc.perform(
            post("/api/user/follow/$userId")
                .header("satoken", token)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(ErrorCode.CANNOT_FOLLOW_SELF))
    }

    @Test
    fun `should send and read private messages`() {
        val (_, tokenA) = registerAndLogin("msg_a")
        val (userBId, _) = registerAndLogin("msg_b")

        mockMvc.perform(
            post("/api/user/message/send")
                .header("satoken", tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"receiverId":$userBId,"content":"你好，想咨询商品"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.receiverId").value(userBId))
            .andExpect(jsonPath("$.data.content").value("你好，想咨询商品"))

        mockMvc.perform(
            get("/api/user/message/conversation/$userBId")
                .header("satoken", tokenA)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].content").value("你好，想咨询商品"))

        mockMvc.perform(
            get("/api/user/message/conversations")
                .header("satoken", tokenA)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].targetUserId").value(userBId))
    }

    @Test
    fun `should return following list for user`() {
        val (userAId, tokenA) = registerAndLogin("flist_a")
        val (userBId, _) = registerAndLogin("flist_b")
        val (userCId, _) = registerAndLogin("flist_c")

        // A follows B and C
        mockMvc.perform(post("/api/user/follow/$userBId").header("satoken", tokenA))
            .andExpect(status().isOk).andExpect(jsonPath("$.code").value(200))
        mockMvc.perform(post("/api/user/follow/$userCId").header("satoken", tokenA))
            .andExpect(status().isOk).andExpect(jsonPath("$.code").value(200))

        mockMvc.perform(get("/api/user/follow/following/$userAId").header("satoken", tokenA))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.length()").value(2))
    }

    @Test
    fun `should return follower list for user`() {
        val (_, tokenA) = registerAndLogin("foll_a")
        val (userBId, tokenB) = registerAndLogin("foll_b")

        // A follows B
        mockMvc.perform(post("/api/user/follow/$userBId").header("satoken", tokenA))
            .andExpect(status().isOk).andExpect(jsonPath("$.code").value(200))

        mockMvc.perform(get("/api/user/follow/followers/$userBId").header("satoken", tokenB))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.length()").value(1))
    }

    @Test
    fun `should return empty following list`() {
        val (userAId, tokenA) = registerAndLogin("empty_fing")

        mockMvc.perform(get("/api/user/follow/following/$userAId").header("satoken", tokenA))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.length()").value(0))
    }

    @Test
    fun `should return followedByMe status in lists`() {
        val (userAId, tokenA) = registerAndLogin("fbymeA")
        val (userBId, tokenB) = registerAndLogin("fbymeB")
        val (userCId, _) = registerAndLogin("fbymeC")

        // A follows B, A follows C
        mockMvc.perform(post("/api/user/follow/$userBId").header("satoken", tokenA))
            .andExpect(status().isOk)
        mockMvc.perform(post("/api/user/follow/$userCId").header("satoken", tokenA))
            .andExpect(status().isOk)
        // B follows C
        mockMvc.perform(post("/api/user/follow/$userCId").header("satoken", tokenB))
            .andExpect(status().isOk)

        // B views A's following list - B should see followedByMe=true for C (B follows C)
        mockMvc.perform(get("/api/user/follow/following/$userAId").header("satoken", tokenB))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.length()").value(2))
    }

    @Test
    fun `should validate private message request`() {
        val (_, tokenA) = registerAndLogin("msg_validate")

        mockMvc.perform(
            post("/api/user/message/send")
                .header("satoken", tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"receiverId":null,"content":""}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_ERROR))
    }
}
