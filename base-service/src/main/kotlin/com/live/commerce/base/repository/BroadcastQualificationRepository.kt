package com.live.commerce.base.repository

import com.live.commerce.base.entity.BroadcastQualification
import org.springframework.data.jpa.repository.JpaRepository

interface BroadcastQualificationRepository : JpaRepository<BroadcastQualification, Long> {
    fun findByUserId(userId: Long): BroadcastQualification?
    fun findByStatus(status: Int): List<BroadcastQualification>
}
