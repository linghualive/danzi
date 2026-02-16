package com.live.commerce.base.dto

import java.time.LocalDateTime

data class LiveRoomDTO(
    val id: Long,
    val userId: Long,
    val title: String,
    val cover: String?,
    val status: Int,
    val streamKey: String,
    val pushUrl: String,
    val pullUrl: String,
    val createdAt: LocalDateTime
)
