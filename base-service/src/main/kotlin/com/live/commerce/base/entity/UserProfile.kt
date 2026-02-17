package com.live.commerce.base.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "user_profile")
class UserProfile(
    @Id
    @Column(name = "user_id")
    var userId: Long = 0,

    @Column(length = 500)
    var bio: String? = null,

    @Column(name = "avatar_file_id")
    var avatarFileId: Long? = null,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
