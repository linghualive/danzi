package com.live.commerce.base.controller

import com.live.commerce.base.TestcontainersConfig
import com.live.commerce.base.entity.ChatMessage
import com.live.commerce.base.repository.ChatMessageRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatMessageControllerIntegrationTest : TestcontainersConfig() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var chatMessageRepository: ChatMessageRepository

    @BeforeEach
    fun setUp() {
        chatMessageRepository.deleteAll()
    }

    @Test
    fun `should return paginated messages for room`() {
        // Insert test data
        val roomId = 1L
        repeat(5) { i ->
            chatMessageRepository.save(
                ChatMessage(
                    roomId = roomId,
                    userId = 100L,
                    nickname = "user1",
                    type = "DANMAKU",
                    content = "message $i"
                )
            )
        }

        mockMvc.perform(
            get("/api/live/room/$roomId/messages")
                .param("page", "0")
                .param("size", "3")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.content.length()").value(3))
            .andExpect(jsonPath("$.data.totalElements").value(5))
            .andExpect(jsonPath("$.data.totalPages").value(2))
            .andExpect(jsonPath("$.data.page").value(0))
            .andExpect(jsonPath("$.data.size").value(3))
    }

    @Test
    fun `should return empty when no messages`() {
        mockMvc.perform(
            get("/api/live/room/999/messages")
                .param("page", "0")
                .param("size", "20")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.content.length()").value(0))
            .andExpect(jsonPath("$.data.totalElements").value(0))
    }
}
