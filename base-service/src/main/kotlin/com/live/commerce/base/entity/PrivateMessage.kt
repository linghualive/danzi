package com.live.commerce.base.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "private_message")
class PrivateMessage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "sender_id", nullable = false)
    var senderId: Long = 0,

    @Column(name = "receiver_id", nullable = false)
    var receiverId: Long = 0,

    @Column(nullable = false, length = 1000)
    var content: String = "",

    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
)
