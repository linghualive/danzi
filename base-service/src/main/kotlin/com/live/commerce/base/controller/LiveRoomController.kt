package com.live.commerce.base.controller

import cn.dev33.satoken.stp.StpUtil
import com.live.commerce.base.dto.CreateRoomRequest
import com.live.commerce.base.dto.LiveRoomDTO
import com.live.commerce.base.service.LiveRoomService
import com.live.commerce.common.dto.PageResult
import com.live.commerce.common.dto.Result
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/live/room")
class LiveRoomController(
    private val liveRoomService: LiveRoomService
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

    @GetMapping("/list")
    fun listRooms(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) status: Int?
    ): Result<PageResult<LiveRoomDTO>> {
        return Result.ok(data = liveRoomService.listRooms(page, size, status))
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
}
