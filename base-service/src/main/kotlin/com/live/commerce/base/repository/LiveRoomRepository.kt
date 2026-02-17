package com.live.commerce.base.repository

import com.live.commerce.base.entity.LiveRoom
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface LiveRoomRepository : JpaRepository<LiveRoom, Long> {
    fun findByStreamKey(streamKey: String): LiveRoom?
    fun findByUserId(userId: Long): List<LiveRoom>
    fun findFirstByUserId(userId: Long): LiveRoom?

    @Query(
        "SELECT r FROM LiveRoom r WHERE r.status = 1 AND (:keyword IS NULL OR r.title LIKE %:keyword%)"
    )
    fun searchLiveRooms(@Param("keyword") keyword: String?, pageable: Pageable): Page<LiveRoom>

    @Query(
        "SELECT r FROM LiveRoom r WHERE (:keyword IS NULL OR r.title LIKE %:keyword%)"
    )
    fun searchAllRooms(@Param("keyword") keyword: String?, pageable: Pageable): Page<LiveRoom>
}
