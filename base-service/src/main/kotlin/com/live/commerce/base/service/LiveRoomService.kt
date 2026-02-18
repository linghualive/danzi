package com.live.commerce.base.service

import com.live.commerce.base.dto.CreateRoomRequest
import com.live.commerce.base.dto.LiveRoomDTO
import com.live.commerce.base.dto.UpdateRoomBasicRequest
import com.live.commerce.common.dto.PageResult

interface LiveRoomService {
    fun createRoom(userId: Long, request: CreateRoomRequest): LiveRoomDTO
    fun getRoomById(id: Long): LiveRoomDTO
    fun getMyRoom(userId: Long): LiveRoomDTO?
    fun updateRoomBasic(roomId: Long, userId: Long, request: UpdateRoomBasicRequest): LiveRoomDTO
    fun listRooms(page: Int, size: Int, keyword: String?): PageResult<LiveRoomDTO>
    fun startLive(roomId: Long, userId: Long): LiveRoomDTO
    fun stopLive(roomId: Long, userId: Long): LiveRoomDTO
    fun adminCloseRoom(roomId: Long, adminId: Long, reason: String): LiveRoomDTO
    fun adminUnbanRoom(roomId: Long, adminId: Long): LiveRoomDTO
}
