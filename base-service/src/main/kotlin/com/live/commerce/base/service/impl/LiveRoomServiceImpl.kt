package com.live.commerce.base.service.impl

import com.live.commerce.base.dto.CreateRoomRequest
import com.live.commerce.base.dto.LiveRoomDTO
import com.live.commerce.base.entity.LiveRoom
import com.live.commerce.base.repository.LiveRoomRepository
import com.live.commerce.base.repository.UserRepository
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
    private val liveRoomRepository: LiveRoomRepository,
    private val broadcastQualificationService: com.live.commerce.base.service.BroadcastQualificationService? = null,
    private val userRepository: UserRepository? = null
) : LiveRoomService {

    @Transactional
    override fun createRoom(userId: Long, request: CreateRoomRequest): LiveRoomDTO {
        // Check qualification before creating new room (skip if updating existing or if user is admin)
        if (liveRoomRepository.findFirstByUserId(userId) == null) {
            val isAdmin = userRepository?.findById(userId)?.map { it.role == 2 }?.orElse(false) ?: false
            if (!isAdmin && broadcastQualificationService != null && !broadcastQualificationService.isQualified(userId)) {
                throw BusinessException(ErrorCode.QUALIFICATION_NOT_APPROVED)
            }
        }

        liveRoomRepository.findFirstByUserId(userId)?.let { existing ->
            existing.title = request.title
            existing.coverFileId = request.coverFileId
            if (existing.status == 2 || existing.status == 3) {
                existing.status = 0
                existing.closedReason = null
                existing.closedBy = null
                existing.closedAt = null
            }
            existing.updatedAt = LocalDateTime.now()
            return toDTO(liveRoomRepository.save(existing))
        }

        val room = LiveRoom(
            userId = userId,
            title = request.title,
            coverFileId = request.coverFileId,
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

    override fun listRooms(page: Int, size: Int, keyword: String?): PageResult<LiveRoomDTO> {
        val pageable = PageRequest.of(page, size)
        val normalizedKeyword = keyword?.trim()?.ifBlank { null }
        val pageResult = liveRoomRepository.searchLiveRooms(normalizedKeyword, pageable)

        return PageResult(
            content = pageResult.content.map { toDTO(it) },
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

        if (room.status == 3) {
            throw BusinessException(ErrorCode.ROOM_CLOSED_BY_ADMIN)
        }
        if (room.status == 1) {
            throw BusinessException(ErrorCode.ROOM_STATUS_ERROR, "直播间已在直播中")
        }

        room.status = 1
        room.startedAt = LocalDateTime.now()
        room.stoppedAt = null
        room.updatedAt = LocalDateTime.now()
        val saved = liveRoomRepository.save(room)
        return toDTO(saved)
    }

    @Transactional
    override fun adminCloseRoom(roomId: Long, adminId: Long, reason: String): LiveRoomDTO {
        val room = liveRoomRepository.findById(roomId)
            .orElseThrow { BusinessException(ErrorCode.ROOM_NOT_FOUND) }
        room.status = 3
        room.closedBy = adminId
        room.closedReason = reason.ifBlank { "管理员关闭直播间" }
        room.closedAt = LocalDateTime.now()
        room.updatedAt = LocalDateTime.now()
        return toDTO(liveRoomRepository.save(room))
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
        room.stoppedAt = LocalDateTime.now()
        room.updatedAt = LocalDateTime.now()
        val saved = liveRoomRepository.save(room)
        return toDTO(saved)
    }

    private fun generateStreamKey(): String = UUID.randomUUID().toString().replace("-", "")

    private fun toDTO(room: LiveRoom): LiveRoomDTO = LiveRoomDTO(
        id = room.id,
        userId = room.userId,
        title = room.title,
        coverUrl = room.coverFileId?.let { "/api/base/media/public/$it" } ?: room.cover,
        status = room.status,
        streamKey = room.streamKey,
        pushUrl = "rtmp://localhost:1935/live/${room.streamKey}",
        pullUrl = "http://localhost:8080/live/${room.streamKey}.flv",
        closedReason = room.closedReason,
        startedAt = room.startedAt,
        stoppedAt = room.stoppedAt,
        createdAt = room.createdAt
    )
}
