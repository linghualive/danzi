package com.live.commerce.base.websocket

import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession

class RoomSessionManagerTest {

    private lateinit var roomSessionManager: RoomSessionManager

    @BeforeEach
    fun setUp() {
        roomSessionManager = RoomSessionManager()
    }

    @Test
    fun `should add session to room`() {
        val session = mockk<WebSocketSession>()

        roomSessionManager.addSession(1L, session)

        assertEquals(1, roomSessionManager.getSessionCount(1L))
    }

    @Test
    fun `should remove session from room`() {
        val session = mockk<WebSocketSession>()

        roomSessionManager.addSession(1L, session)
        assertEquals(1, roomSessionManager.getSessionCount(1L))

        roomSessionManager.removeSession(1L, session)
        assertEquals(0, roomSessionManager.getSessionCount(1L))
    }

    @Test
    fun `should broadcast message to all sessions in room`() {
        val session1 = mockk<WebSocketSession>()
        val session2 = mockk<WebSocketSession>()
        val session3 = mockk<WebSocketSession>() // different room

        every { session1.isOpen } returns true
        every { session2.isOpen } returns true
        every { session3.isOpen } returns true
        every { session1.sendMessage(any()) } just runs
        every { session2.sendMessage(any()) } just runs
        every { session3.sendMessage(any()) } just runs

        roomSessionManager.addSession(1L, session1)
        roomSessionManager.addSession(1L, session2)
        roomSessionManager.addSession(2L, session3)

        roomSessionManager.broadcast(1L, "hello")

        verify(exactly = 1) { session1.sendMessage(match<TextMessage> { it.payload == "hello" }) }
        verify(exactly = 1) { session2.sendMessage(match<TextMessage> { it.payload == "hello" }) }
        verify(exactly = 0) { session3.sendMessage(any()) }
    }

    @Test
    fun `should return correct session count`() {
        val session1 = mockk<WebSocketSession>()
        val session2 = mockk<WebSocketSession>()

        assertEquals(0, roomSessionManager.getSessionCount(1L))

        roomSessionManager.addSession(1L, session1)
        assertEquals(1, roomSessionManager.getSessionCount(1L))

        roomSessionManager.addSession(1L, session2)
        assertEquals(2, roomSessionManager.getSessionCount(1L))

        // Different room should not affect the count
        assertEquals(0, roomSessionManager.getSessionCount(2L))
    }
}
