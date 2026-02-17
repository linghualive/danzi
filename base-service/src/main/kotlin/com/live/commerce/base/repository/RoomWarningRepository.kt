package com.live.commerce.base.repository

import com.live.commerce.base.entity.RoomWarning
import org.springframework.data.jpa.repository.JpaRepository

interface RoomWarningRepository : JpaRepository<RoomWarning, Long> {
    fun findByRoomIdOrderByCreatedAtDesc(roomId: Long): List<RoomWarning>
}
