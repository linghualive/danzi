package com.live.commerce.base.websocket

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.live.commerce.base.service.ChatMessageService
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.net.URI

class LiveWebSocketHandlerTest {

    private val roomSessionManager = mockk<RoomSessionManager>(relaxed = true)
    private val chatMessageService = mockk<ChatMessageService>(relaxed = true)
    private lateinit var liveWebSocketHandler: LiveWebSocketHandler
    private val objectMapper = jacksonObjectMapper()

    @BeforeEach
    fun setUp() {
        liveWebSocketHandler = LiveWebSocketHandler(roomSessionManager, objectMapper, chatMessageService)
    }

    @Test
    fun `should add session on connection established`() {
        val session = mockk<WebSocketSession>()
        every { session.uri } returns URI("/ws/live/42")

        liveWebSocketHandler.afterConnectionEstablished(session)

        verify { roomSessionManager.addSession(42L, session) }
    }

    @Test
    fun `should broadcast message on text message received`() {
        val session = mockk<WebSocketSession>()
        every { session.uri } returns URI("/ws/live/42")

        val message = LiveMessage(
            type = MessageType.DANMAKU,
            userId = 100L,
            nickname = "testUser",
            content = "Hello World",
            roomId = 42L
        )
        val messageJson = objectMapper.writeValueAsString(message)
        val textMessage = TextMessage(messageJson)

        liveWebSocketHandler.handleMessage(session, textMessage)

        verify { roomSessionManager.broadcast(42L, any()) }
    }

    @Test
    fun `should remove session on connection closed`() {
        val session = mockk<WebSocketSession>()
        every { session.uri } returns URI("/ws/live/42")

        liveWebSocketHandler.afterConnectionClosed(session, CloseStatus.NORMAL)

        verify { roomSessionManager.removeSession(42L, session) }
    }

    @Test
    fun `should save message when handling text message`() {
        val session = mockk<WebSocketSession>()
        every { session.uri } returns URI("/ws/live/42")

        val message = LiveMessage(
            type = MessageType.DANMAKU,
            userId = 100L,
            nickname = "testUser",
            content = "Hello World",
            roomId = 42L
        )
        val messageJson = objectMapper.writeValueAsString(message)
        val textMessage = TextMessage(messageJson)

        liveWebSocketHandler.handleMessage(session, textMessage)

        verify { chatMessageService.saveMessage(42L, 100L, "testUser", MessageType.DANMAKU, "Hello World") }
    }

    @Test
    fun `should not save like message`() {
        val session = mockk<WebSocketSession>()
        every { session.uri } returns URI("/ws/live/42")

        val message = LiveMessage(
            type = MessageType.LIKE,
            userId = 100L,
            nickname = "testUser",
            content = "",
            roomId = 42L
        )
        val messageJson = objectMapper.writeValueAsString(message)
        val textMessage = TextMessage(messageJson)

        liveWebSocketHandler.handleMessage(session, textMessage)

        verify(exactly = 0) { chatMessageService.saveMessage(any(), any(), any(), any(), any()) }
    }
}
