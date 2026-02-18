package com.live.commerce.base.dto

import java.time.LocalDateTime

data class BroadcastQualificationDTO(
    val id: Long,
    val userId: Long,
    val contactInfo: String,
    val businessLicense: String?,
    val personalInfo: String?,
    val status: Int,
    val rejectReason: String?,
    val reviewedBy: Long?,
    val reviewedAt: LocalDateTime?,
    val createdAt: LocalDateTime
)
