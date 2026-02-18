package com.live.commerce.base.dto

import java.time.LocalDateTime

data class LiveRoomDTO(
    val id: Long,
    val userId: Long,
    val title: String,
    val coverUrl: String?,
    val status: Int,
    val streamKey: String,
    val pushUrl: String,
    val pullUrl: String,
    val closedReason: String?,
    val startedAt: LocalDateTime? = null,
    val stoppedAt: LocalDateTime? = null,
    val createdAt: LocalDateTime,
    val anchorName: String? = null,
    val viewerCount: Int = 0
)
