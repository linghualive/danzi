package com.live.commerce.base.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "room_warning")
class RoomWarning(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "room_id", nullable = false)
    var roomId: Long = 0,

    @Column(name = "admin_id", nullable = false)
    var adminId: Long = 0,

    @Column(nullable = false, length = 255)
    var message: String = "",

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
)
