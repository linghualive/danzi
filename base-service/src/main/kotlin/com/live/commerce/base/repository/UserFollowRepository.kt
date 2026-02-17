package com.live.commerce.base.repository

import com.live.commerce.base.entity.UserFollow
import org.springframework.data.jpa.repository.JpaRepository

interface UserFollowRepository : JpaRepository<UserFollow, Long> {
    fun existsByFollowerIdAndFolloweeId(followerId: Long, followeeId: Long): Boolean
    fun deleteByFollowerIdAndFolloweeId(followerId: Long, followeeId: Long): Long
    fun countByFollowerId(followerId: Long): Long
    fun countByFolloweeId(followeeId: Long): Long
}
