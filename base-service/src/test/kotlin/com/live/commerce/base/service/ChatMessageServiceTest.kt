package com.live.commerce.base.service

import com.live.commerce.base.dto.ChatMessageDTO
import com.live.commerce.base.entity.ChatMessage
import com.live.commerce.base.repository.ChatMessageRepository
import com.live.commerce.base.service.impl.ChatMessageServiceImpl
import com.live.commerce.base.websocket.MessageType
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.LocalDateTime

class ChatMessageServiceTest {

    private val chatMessageRepository = mockk<ChatMessageRepository>()
    private lateinit var chatMessageService: ChatMessageServiceImpl

    @BeforeEach
    fun setUp() {
        chatMessageService = ChatMessageServiceImpl(chatMessageRepository)
    }

    @Test
    fun `should save danmaku message`() {
        val slot = slot<ChatMessage>()
        every { chatMessageRepository.save(capture(slot)) } answers {
            slot.captured.apply { id = 1L }
        }

        chatMessageService.saveMessage(
            roomId = 1L,
            userId = 100L,
            nickname = "user1",
            type = MessageType.DANMAKU,
            content = "Hello World"
        )

        verify(exactly = 1) { chatMessageRepository.save(any()) }
        assertEquals(1L, slot.captured.roomId)
        assertEquals(100L, slot.captured.userId)
        assertEquals("user1", slot.captured.nickname)
        assertEquals("DANMAKU", slot.captured.type)
        assertEquals("Hello World", slot.captured.content)
    }

    @Test
    fun `should save comment message`() {
        val slot = slot<ChatMessage>()
        every { chatMessageRepository.save(capture(slot)) } answers {
            slot.captured.apply { id = 2L }
        }

        chatMessageService.saveMessage(
            roomId = 1L,
            userId = 200L,
            nickname = "user2",
            type = MessageType.COMMENT,
            content = "Nice product!"
        )

        verify(exactly = 1) { chatMessageRepository.save(any()) }
        assertEquals("COMMENT", slot.captured.type)
        assertEquals("Nice product!", slot.captured.content)
    }

    @Test
    fun `should return messages by room id with pagination`() {
        val now = LocalDateTime.now()
        val messages = listOf(
            ChatMessage(id = 1L, roomId = 1L, userId = 100L, nickname = "user1", type = "DANMAKU", content = "msg1", createdAt = now),
            ChatMessage(id = 2L, roomId = 1L, userId = 200L, nickname = "user2", type = "COMMENT", content = "msg2", createdAt = now.plusSeconds(1))
        )
        val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
        every { chatMessageRepository.findByRoomId(1L, pageable) } returns PageImpl(messages, pageable, 2L)

        val result = chatMessageService.getMessages(1L, 0, 20)

        assertEquals(2, result.content.size)
        assertEquals(0, result.page)
        assertEquals(20, result.size)
        assertEquals(2L, result.totalElements)
        assertEquals("msg1", result.content[0].content)
        assertEquals("msg2", result.content[1].content)
    }
}
