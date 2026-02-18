package com.live.commerce.base.controller

import cn.dev33.satoken.stp.StpUtil
import com.live.commerce.base.dto.CreateRoomRequest
import com.live.commerce.base.dto.LiveRoomDTO
import com.live.commerce.base.dto.RoomWarningDTO
import com.live.commerce.base.dto.UpdateRoomBasicRequest
import com.live.commerce.base.repository.LiveRoomRepository
import com.live.commerce.base.repository.RoomWarningRepository
import com.live.commerce.base.service.LiveRoomService
import com.live.commerce.base.support.UserPermissionSupport
import com.live.commerce.common.dto.PageResult
import com.live.commerce.common.dto.Result
import com.live.commerce.common.exception.BusinessException
import com.live.commerce.common.exception.ErrorCode
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/live/room")
class LiveRoomController(
    private val liveRoomService: LiveRoomService,
    private val liveRoomRepository: LiveRoomRepository,
    private val roomWarningRepository: RoomWarningRepository,
    private val userPermissionSupport: UserPermissionSupport
) {

    @PostMapping
    fun createRoom(@Valid @RequestBody request: CreateRoomRequest): Result<LiveRoomDTO> {
        val userId = StpUtil.getLoginIdAsLong()
        return Result.ok(data = liveRoomService.createRoom(userId, request))
    }

    @GetMapping("/{id}")
    fun getRoom(@PathVariable id: Long): Result<LiveRoomDTO> {
        return Result.ok(data = liveRoomService.getRoomById(id))
    }

    @GetMapping("/my")
    fun getMyRoom(): Result<LiveRoomDTO?> {
        val userId = StpUtil.getLoginIdAsLong()
        return Result.ok(data = liveRoomService.getMyRoom(userId))
    }

    @GetMapping("/list")
    fun listRooms(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) keyword: String?
    ): Result<PageResult<LiveRoomDTO>> {
        return Result.ok(data = liveRoomService.listRooms(page, size, keyword))
    }

    @PutMapping("/{id}/start")
    fun startLive(@PathVariable id: Long): Result<LiveRoomDTO> {
        val userId = StpUtil.getLoginIdAsLong()
        return Result.ok(data = liveRoomService.startLive(id, userId))
    }

    @PutMapping("/{id}/stop")
    fun stopLive(@PathVariable id: Long): Result<LiveRoomDTO> {
        val userId = StpUtil.getLoginIdAsLong()
        return Result.ok(data = liveRoomService.stopLive(id, userId))
    }

    @PutMapping("/{id}/basic")
    fun updateRoomBasic(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateRoomBasicRequest
    ): Result<LiveRoomDTO> {
        val userId = StpUtil.getLoginIdAsLong()
        return Result.ok(data = liveRoomService.updateRoomBasic(id, userId, request))
    }

    @GetMapping("/{id}/warnings")
    fun getRoomWarnings(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "20") size: Int
    ): Result<List<RoomWarningDTO>> {
        val userId = StpUtil.getLoginIdAsLong()
        val room = liveRoomRepository.findById(id)
            .orElseThrow { BusinessException(ErrorCode.ROOM_NOT_FOUND) }

        if (room.userId != userId) {
            userPermissionSupport.requireAdmin(userId)
        }

        val normalizedSize = size.coerceIn(1, 100)
        val warnings = roomWarningRepository.findByRoomIdOrderByCreatedAtDesc(id)
            .take(normalizedSize)
            .map {
                RoomWarningDTO(
                    id = it.id,
                    roomId = it.roomId,
                    adminId = it.adminId,
                    message = it.message,
                    createdAt = it.createdAt
                )
            }

        return Result.ok(data = warnings)
    }
}
