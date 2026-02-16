package com.live.commerce.base.service

import com.live.commerce.base.dto.SrsCallbackRequest
import com.live.commerce.base.entity.LiveRoom
import com.live.commerce.base.repository.LiveRoomRepository
import com.live.commerce.base.service.impl.LiveCallbackServiceImpl
import com.live.commerce.common.exception.BusinessException
import com.live.commerce.common.exception.ErrorCode
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LiveCallbackServiceTest {

    private val liveRoomRepository = mockk<LiveRoomRepository>()
    private lateinit var liveCallbackService: LiveCallbackServiceImpl

    @BeforeEach
    fun setUp() {
        liveCallbackService = LiveCallbackServiceImpl(liveRoomRepository)
    }

    @Test
    fun `should update room status to live on publish`() {
        val room = LiveRoom(id = 1L, userId = 100L, title = "Test Room", streamKey = "stream123", status = 0)
        every { liveRoomRepository.findByStreamKey("stream123") } returns room
        every { liveRoomRepository.save(any()) } answers { firstArg() }

        val request = SrsCallbackRequest(
            action = "on_publish",
            ip = "127.0.0.1",
            vhost = "__defaultVhost__",
            app = "live",
            stream = "stream123",
            param = ""
        )

        liveCallbackService.onPublish(request)

        verify { liveRoomRepository.save(match { it.status == 1 }) }
    }

    @Test
    fun `should throw exception when stream key not found on publish`() {
        every { liveRoomRepository.findByStreamKey("unknown_key") } returns null

        val request = SrsCallbackRequest(
            action = "on_publish",
            ip = "127.0.0.1",
            vhost = "__defaultVhost__",
            app = "live",
            stream = "unknown_key",
            param = ""
        )

        val exception = assertThrows<BusinessException> {
            liveCallbackService.onPublish(request)
        }
        assertEquals(ErrorCode.ROOM_NOT_FOUND, exception.code)
    }

    @Test
    fun `should update room status to ended on unpublish`() {
        val room = LiveRoom(id = 1L, userId = 100L, title = "Test Room", streamKey = "stream123", status = 1)
        every { liveRoomRepository.findByStreamKey("stream123") } returns room
        every { liveRoomRepository.save(any()) } answers { firstArg() }

        val request = SrsCallbackRequest(
            action = "on_unpublish",
            ip = "127.0.0.1",
            vhost = "__defaultVhost__",
            app = "live",
            stream = "stream123",
            param = ""
        )

        liveCallbackService.onUnpublish(request)

        verify { liveRoomRepository.save(match { it.status == 2 }) }
    }

    @Test
    fun `should throw exception when stream key not found on unpublish`() {
        every { liveRoomRepository.findByStreamKey("unknown_key") } returns null

        val request = SrsCallbackRequest(
            action = "on_unpublish",
            ip = "127.0.0.1",
            vhost = "__defaultVhost__",
            app = "live",
            stream = "unknown_key",
            param = ""
        )

        val exception = assertThrows<BusinessException> {
            liveCallbackService.onUnpublish(request)
        }
        assertEquals(ErrorCode.ROOM_NOT_FOUND, exception.code)
    }
}
