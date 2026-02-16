package com.live.commerce.base.service

import com.live.commerce.base.dto.CreateRoomRequest
import com.live.commerce.base.dto.LiveRoomDTO
import com.live.commerce.common.dto.PageResult

interface LiveRoomService {
    fun createRoom(userId: Long, request: CreateRoomRequest): LiveRoomDTO
    fun getRoomById(id: Long): LiveRoomDTO
    fun listRooms(page: Int, size: Int, status: Int?): PageResult<LiveRoomDTO>
    fun startLive(roomId: Long, userId: Long): LiveRoomDTO
    fun stopLive(roomId: Long, userId: Long): LiveRoomDTO
}
