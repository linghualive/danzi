package com.live.commerce.base.repository

import com.live.commerce.base.entity.UserProfile
import org.springframework.data.jpa.repository.JpaRepository

interface UserProfileRepository : JpaRepository<UserProfile, Long> {
    fun findByUserId(userId: Long): UserProfile?
}
