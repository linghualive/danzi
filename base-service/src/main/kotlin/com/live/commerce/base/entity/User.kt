package com.live.commerce.base.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "`user`")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(nullable = false, unique = true, length = 50)
    var username: String = "",

    @Column(nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    var password: String = "",

    @Column(nullable = false, length = 50)
    var nickname: String = "",

    var avatar: String? = null,

    @Column(nullable = false)
    var role: Int = 0,

    @Column(nullable = false)
    var status: Int = 0, // 0=normal, 1=disabled

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
