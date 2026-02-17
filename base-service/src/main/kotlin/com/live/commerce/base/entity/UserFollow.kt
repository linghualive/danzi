package com.live.commerce.base.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "user_follow",
    uniqueConstraints = [UniqueConstraint(columnNames = ["follower_id", "followee_id"])]
)
class UserFollow(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "follower_id", nullable = false)
    var followerId: Long = 0,

    @Column(name = "followee_id", nullable = false)
    var followeeId: Long = 0,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
)
