package com.live.commerce.base.repository

import com.live.commerce.base.entity.LiveRoom
import org.springframework.data.jpa.repository.JpaRepository

interface LiveRoomRepository : JpaRepository<LiveRoom, Long> {
    fun findByStreamKey(streamKey: String): LiveRoom?
    fun findByStatus(status: Int): List<LiveRoom>
    fun findByUserId(userId: Long): List<LiveRoom>
}
