package com.live.commerce.base.service

import com.live.commerce.base.dto.CreateRoomRequest
import com.live.commerce.base.entity.LiveRoom
import com.live.commerce.base.repository.LiveRoomRepository
import com.live.commerce.base.service.impl.LiveRoomServiceImpl
import com.live.commerce.common.exception.BusinessException
import com.live.commerce.common.exception.ErrorCode
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime
import java.util.Optional

class LiveRoomServiceTest {

    private val liveRoomRepository = mockk<LiveRoomRepository>()
    private lateinit var liveRoomService: LiveRoomServiceImpl

    @BeforeEach
    fun setUp() {
        liveRoomService = LiveRoomServiceImpl(liveRoomRepository)
    }

    @Test
    fun `should create room with generated stream key`() {
        val request = CreateRoomRequest("Test Room")
        val roomSlot = slot<LiveRoom>()
        every { liveRoomRepository.findFirstByUserId(100L) } returns null
        every { liveRoomRepository.save(capture(roomSlot)) } answers {
            roomSlot.captured.apply { id = 1L }
        }

        val result = liveRoomService.createRoom(100L, request)

        assertEquals(1L, result.id)
        assertEquals("Test Room", result.title)
        assertEquals(100L, result.userId)
        assertTrue(result.streamKey.isNotBlank())
        assertTrue(result.pushUrl.contains(result.streamKey))
        assertTrue(result.pullUrl.contains(result.streamKey))
    }

    @Test
    fun `should reuse existing room for same user`() {
        val existing = LiveRoom(
            id = 10L,
            userId = 100L,
            title = "Old Title",
            streamKey = "fixed-key",
            coverFileId = null
        )
        every { liveRoomRepository.findFirstByUserId(100L) } returns existing
        every { liveRoomRepository.save(any()) } answers { firstArg() }

        val result = liveRoomService.createRoom(100L, CreateRoomRequest("New Title", coverFileId = 55L))

        assertEquals(10L, result.id)
        assertEquals("New Title", result.title)
        assertEquals("fixed-key", result.streamKey)
        assertEquals("/api/base/media/public/55", result.coverUrl)
        verify {
            liveRoomRepository.save(match {
                it.id == 10L && it.title == "New Title" && it.coverFileId == 55L
            })
        }
    }

    @Test
    fun `should get room by id`() {
        val room = LiveRoom(id = 1L, userId = 100L, title = "Test Room", streamKey = "abc123")
        every { liveRoomRepository.findById(1L) } returns Optional.of(room)

        val result = liveRoomService.getRoomById(1L)

        assertEquals(1L, result.id)
        assertEquals("Test Room", result.title)
    }

    @Test
    fun `should throw exception when room not found`() {
        every { liveRoomRepository.findById(99L) } returns Optional.empty()

        val exception = assertThrows<BusinessException> { liveRoomService.getRoomById(99L) }
        assertEquals(ErrorCode.ROOM_NOT_FOUND, exception.code)
    }

    @Test
    fun `should list rooms with pagination`() {
        val rooms = listOf(
            LiveRoom(id = 1L, userId = 100L, title = "Room 1", streamKey = "key1", status = 1),
            LiveRoom(id = 2L, userId = 101L, title = "Room 2", streamKey = "key2")
        )
        val pageable = PageRequest.of(0, 10)
        every { liveRoomRepository.searchLiveRooms(null, pageable) } returns PageImpl(rooms, pageable, 2)

        val result = liveRoomService.listRooms(0, 10, null)

        assertEquals(2, result.content.size)
        assertEquals(2L, result.totalElements)
    }

    @Test
    fun `should start live successfully`() {
        val room = LiveRoom(id = 1L, userId = 100L, title = "Test Room", streamKey = "abc123", status = 0)
        every { liveRoomRepository.findById(1L) } returns Optional.of(room)
        every { liveRoomRepository.save(any()) } answers { firstArg() }

        val result = liveRoomService.startLive(1L, 100L)

        assertEquals(1, result.status)
    }

    @Test
    fun `should throw exception when non-owner tries to start live`() {
        val room = LiveRoom(id = 1L, userId = 100L, title = "Test Room", streamKey = "abc123", status = 0)
        every { liveRoomRepository.findById(1L) } returns Optional.of(room)

        val exception = assertThrows<BusinessException> { liveRoomService.startLive(1L, 999L) }
        assertEquals(ErrorCode.ROOM_PERMISSION_DENIED, exception.code)
    }

    @Test
    fun `should throw exception when starting already live room`() {
        val room = LiveRoom(id = 1L, userId = 100L, title = "Test Room", streamKey = "abc123", status = 1)
        every { liveRoomRepository.findById(1L) } returns Optional.of(room)

        val exception = assertThrows<BusinessException> { liveRoomService.startLive(1L, 100L) }
        assertEquals(ErrorCode.ROOM_STATUS_ERROR, exception.code)
    }

    @Test
    fun `should reject start when room is closed by admin`() {
        val room = LiveRoom(id = 1L, userId = 100L, title = "Test Room", streamKey = "abc123", status = 3)
        every { liveRoomRepository.findById(1L) } returns Optional.of(room)

        val exception = assertThrows<BusinessException> { liveRoomService.startLive(1L, 100L) }
        assertEquals(ErrorCode.ROOM_CLOSED_BY_ADMIN, exception.code)
    }

    @Test
    fun `should stop live successfully`() {
        val room = LiveRoom(id = 1L, userId = 100L, title = "Test Room", streamKey = "abc123", status = 1)
        every { liveRoomRepository.findById(1L) } returns Optional.of(room)
        every { liveRoomRepository.save(any()) } answers { firstArg() }

        val result = liveRoomService.stopLive(1L, 100L)

        assertEquals(2, result.status)
    }

    @Test
    fun `should throw exception when non-owner tries to stop live`() {
        val room = LiveRoom(id = 1L, userId = 100L, title = "Test Room", streamKey = "abc123", status = 1)
        every { liveRoomRepository.findById(1L) } returns Optional.of(room)

        val exception = assertThrows<BusinessException> { liveRoomService.stopLive(1L, 999L) }
        assertEquals(ErrorCode.ROOM_PERMISSION_DENIED, exception.code)
    }

    @Test
    fun `should throw exception when stopping non-live room`() {
        val room = LiveRoom(id = 1L, userId = 100L, title = "Test Room", streamKey = "abc123", status = 0)
        every { liveRoomRepository.findById(1L) } returns Optional.of(room)

        val exception = assertThrows<BusinessException> { liveRoomService.stopLive(1L, 100L) }
        assertEquals(ErrorCode.ROOM_STATUS_ERROR, exception.code)
    }

    @Test
    fun `should record startedAt when starting live`() {
        val room = LiveRoom(id = 1L, userId = 100L, title = "Test Room", streamKey = "abc123", status = 0)
        every { liveRoomRepository.findById(1L) } returns Optional.of(room)
        every { liveRoomRepository.save(any()) } answers { firstArg() }

        val result = liveRoomService.startLive(1L, 100L)

        assertNotNull(result.startedAt)
        assertNull(result.stoppedAt)
    }

    @Test
    fun `should record stoppedAt when stopping live`() {
        val room = LiveRoom(id = 1L, userId = 100L, title = "Test Room", streamKey = "abc123", status = 1, startedAt = LocalDateTime.now().minusHours(1))
        every { liveRoomRepository.findById(1L) } returns Optional.of(room)
        every { liveRoomRepository.save(any()) } answers { firstArg() }

        val result = liveRoomService.stopLive(1L, 100L)

        assertNotNull(result.stoppedAt)
        assertNotNull(result.startedAt)
    }

    @Test
    fun `should throw exception when starting non-existent room`() {
        every { liveRoomRepository.findById(99L) } returns Optional.empty()

        val exception = assertThrows<BusinessException> { liveRoomService.startLive(99L, 100L) }
        assertEquals(ErrorCode.ROOM_NOT_FOUND, exception.code)
    }
}
