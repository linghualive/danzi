package com.live.commerce.base.service.impl

import com.live.commerce.base.dto.BroadcastQualificationDTO
import com.live.commerce.base.dto.ReviewQualificationRequest
import com.live.commerce.base.dto.SubmitQualificationRequest
import com.live.commerce.base.entity.BroadcastQualification
import com.live.commerce.base.repository.BroadcastQualificationRepository
import com.live.commerce.base.service.BroadcastQualificationService
import com.live.commerce.common.exception.BusinessException
import com.live.commerce.common.exception.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class BroadcastQualificationServiceImpl(
    private val qualificationRepository: BroadcastQualificationRepository
) : BroadcastQualificationService {

    @Transactional
    override fun submit(userId: Long, request: SubmitQualificationRequest): BroadcastQualificationDTO {
        val existing = qualificationRepository.findByUserId(userId)
        if (existing != null) {
            if (existing.status == 2) {
                // Allow re-submission for rejected applications
                existing.contactInfo = request.contactInfo
                existing.businessLicense = request.businessLicense
                existing.personalInfo = request.personalInfo
                existing.status = 0
                existing.rejectReason = null
                existing.reviewedBy = null
                existing.reviewedAt = null
                existing.updatedAt = LocalDateTime.now()
                return toDTO(qualificationRepository.save(existing))
            }
            throw BusinessException(ErrorCode.QUALIFICATION_ALREADY_SUBMITTED)
        }

        val qualification = BroadcastQualification(
            userId = userId,
            contactInfo = request.contactInfo,
            businessLicense = request.businessLicense,
            personalInfo = request.personalInfo
        )
        val saved = qualificationRepository.save(qualification)
        return toDTO(saved)
    }

    override fun getMyQualification(userId: Long): BroadcastQualificationDTO? {
        return qualificationRepository.findByUserId(userId)?.let { toDTO(it) }
    }

    @Transactional
    override fun review(qualificationId: Long, adminId: Long, request: ReviewQualificationRequest): BroadcastQualificationDTO {
        val qualification = qualificationRepository.findById(qualificationId)
            .orElseThrow { BusinessException(ErrorCode.QUALIFICATION_NOT_FOUND) }

        qualification.status = request.status
        qualification.rejectReason = if (request.status == 2) request.rejectReason else null
        qualification.reviewedBy = adminId
        qualification.reviewedAt = LocalDateTime.now()
        qualification.updatedAt = LocalDateTime.now()

        return toDTO(qualificationRepository.save(qualification))
    }

    override fun getPendingApplications(): List<BroadcastQualificationDTO> {
        return qualificationRepository.findAll().map { toDTO(it) }
    }

    override fun isQualified(userId: Long): Boolean {
        val qualification = qualificationRepository.findByUserId(userId) ?: return false
        return qualification.status == 1
    }

    private fun toDTO(q: BroadcastQualification): BroadcastQualificationDTO = BroadcastQualificationDTO(
        id = q.id,
        userId = q.userId,
        contactInfo = q.contactInfo,
        businessLicense = q.businessLicense,
        personalInfo = q.personalInfo,
        status = q.status,
        rejectReason = q.rejectReason,
        reviewedBy = q.reviewedBy,
        reviewedAt = q.reviewedAt,
        createdAt = q.createdAt
    )
}
