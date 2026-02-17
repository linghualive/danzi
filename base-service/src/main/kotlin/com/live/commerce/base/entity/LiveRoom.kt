package com.live.commerce.base.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "live_room")
class LiveRoom(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "user_id", nullable = false, unique = true)
    var userId: Long = 0,

    @Column(nullable = false, length = 100)
    var title: String = "",

    var cover: String? = null,

    @Column(name = "cover_file_id")
    var coverFileId: Long? = null,

    @Column(nullable = false)
    var status: Int = 0, // 0-未开播 1-直播中 2-已结束 3-已被管理员关闭

    @Column(name = "stream_key", nullable = false, unique = true, length = 64)
    var streamKey: String = "",

    @Column(name = "closed_reason", length = 255)
    var closedReason: String? = null,

    @Column(name = "closed_by")
    var closedBy: Long? = null,

    @Column(name = "closed_at")
    var closedAt: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
