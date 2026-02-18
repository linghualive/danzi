package com.live.commerce.base.service

import com.live.commerce.base.dto.BroadcastQualificationDTO
import com.live.commerce.base.dto.ReviewQualificationRequest
import com.live.commerce.base.dto.SubmitQualificationRequest

interface BroadcastQualificationService {
    fun submit(userId: Long, request: SubmitQualificationRequest): BroadcastQualificationDTO
    fun getMyQualification(userId: Long): BroadcastQualificationDTO?
    fun review(qualificationId: Long, adminId: Long, request: ReviewQualificationRequest): BroadcastQualificationDTO
    fun getPendingApplications(): List<BroadcastQualificationDTO>
    fun isQualified(userId: Long): Boolean
}
