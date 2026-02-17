package com.live.commerce.base.service.impl

import com.live.commerce.base.dto.SrsCallbackRequest
import com.live.commerce.base.repository.LiveRoomRepository
import com.live.commerce.base.service.LiveCallbackService
import com.live.commerce.common.exception.BusinessException
import com.live.commerce.common.exception.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class LiveCallbackServiceImpl(
    private val liveRoomRepository: LiveRoomRepository
) : LiveCallbackService {

    @Transactional
    override fun onPublish(request: SrsCallbackRequest) {
        val room = liveRoomRepository.findByStreamKey(request.stream)
            ?: throw BusinessException(ErrorCode.ROOM_NOT_FOUND)
        if (room.status == 3) {
            throw BusinessException(ErrorCode.ROOM_CLOSED_BY_ADMIN)
        }
        room.status = 1
        room.updatedAt = LocalDateTime.now()
        liveRoomRepository.save(room)
    }

    @Transactional
    override fun onUnpublish(request: SrsCallbackRequest) {
        val room = liveRoomRepository.findByStreamKey(request.stream)
            ?: throw BusinessException(ErrorCode.ROOM_NOT_FOUND)
        room.status = 2
        room.updatedAt = LocalDateTime.now()
        liveRoomRepository.save(room)
    }
}
