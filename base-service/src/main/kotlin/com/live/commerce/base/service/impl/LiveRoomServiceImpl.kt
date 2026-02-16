package com.live.commerce.base.service.impl

import com.live.commerce.base.dto.CreateRoomRequest
import com.live.commerce.base.dto.LiveRoomDTO
import com.live.commerce.base.entity.LiveRoom
import com.live.commerce.base.repository.LiveRoomRepository
import com.live.commerce.base.service.LiveRoomService
import com.live.commerce.common.dto.PageResult
import com.live.commerce.common.exception.BusinessException
import com.live.commerce.common.exception.ErrorCode
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class LiveRoomServiceImpl(
    private val liveRoomRepository: LiveRoomRepository
) : LiveRoomService {

    @Transactional
    override fun createRoom(userId: Long, request: CreateRoomRequest): LiveRoomDTO {
        val room = LiveRoom(
            userId = userId,
            title = request.title,
            cover = request.cover,
            streamKey = generateStreamKey()
        )
        val saved = liveRoomRepository.save(room)
        return toDTO(saved)
    }

    override fun getRoomById(id: Long): LiveRoomDTO {
        val room = liveRoomRepository.findById(id)
            .orElseThrow { BusinessException(ErrorCode.ROOM_NOT_FOUND) }
        return toDTO(room)
    }

    override fun listRooms(page: Int, size: Int, status: Int?): PageResult<LiveRoomDTO> {
        val pageable = PageRequest.of(page, size)
        val pageResult = if (status != null) {
            liveRoomRepository.findAll(pageable)
        } else {
            liveRoomRepository.findAll(pageable)
        }
        return PageResult(
            content = pageResult.content.let { rooms ->
                if (status != null) rooms.filter { it.status == status }.map { toDTO(it) }
                else rooms.map { toDTO(it) }
            },
            page = pageResult.number,
            size = pageResult.size,
            totalElements = pageResult.totalElements,
            totalPages = pageResult.totalPages
        )
    }

    @Transactional
    override fun startLive(roomId: Long, userId: Long): LiveRoomDTO {
        val room = liveRoomRepository.findById(roomId)
            .orElseThrow { BusinessException(ErrorCode.ROOM_NOT_FOUND) }

        if (room.userId != userId) {
            throw BusinessException(ErrorCode.ROOM_PERMISSION_DENIED)
        }

        if (room.status != 0) {
            throw BusinessException(ErrorCode.ROOM_STATUS_ERROR, "只有未开播的直播间可以开播")
        }

        room.status = 1
        room.updatedAt = LocalDateTime.now()
        val saved = liveRoomRepository.save(room)
        return toDTO(saved)
    }

    @Transactional
    override fun stopLive(roomId: Long, userId: Long): LiveRoomDTO {
        val room = liveRoomRepository.findById(roomId)
            .orElseThrow { BusinessException(ErrorCode.ROOM_NOT_FOUND) }

        if (room.userId != userId) {
            throw BusinessException(ErrorCode.ROOM_PERMISSION_DENIED)
        }

        if (room.status != 1) {
            throw BusinessException(ErrorCode.ROOM_STATUS_ERROR, "只有直播中的直播间可以停播")
        }

        room.status = 2
        room.updatedAt = LocalDateTime.now()
        val saved = liveRoomRepository.save(room)
        return toDTO(saved)
    }

    private fun generateStreamKey(): String = UUID.randomUUID().toString().replace("-", "")

    private fun toDTO(room: LiveRoom): LiveRoomDTO = LiveRoomDTO(
        id = room.id,
        userId = room.userId,
        title = room.title,
        cover = room.cover,
        status = room.status,
        streamKey = room.streamKey,
        pushUrl = "rtmp://localhost:1935/live/${room.streamKey}",
        pullUrl = "http://localhost:8080/live/${room.streamKey}.flv",
        createdAt = room.createdAt
    )
}
