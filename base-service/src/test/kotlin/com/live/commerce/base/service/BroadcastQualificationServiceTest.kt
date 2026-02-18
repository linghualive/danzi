package com.live.commerce.base.service

import com.live.commerce.base.dto.ReviewQualificationRequest
import com.live.commerce.base.dto.SubmitQualificationRequest
import com.live.commerce.base.entity.BroadcastQualification
import com.live.commerce.base.repository.BroadcastQualificationRepository
import com.live.commerce.base.service.impl.BroadcastQualificationServiceImpl
import com.live.commerce.common.exception.BusinessException
import com.live.commerce.common.exception.ErrorCode
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Optional

class BroadcastQualificationServiceTest {

    private val repository = mockk<BroadcastQualificationRepository>()
    private lateinit var service: BroadcastQualificationServiceImpl

    @BeforeEach
    fun setUp() {
        service = BroadcastQualificationServiceImpl(repository)
    }

    @Test
    fun `should submit qualification successfully`() {
        every { repository.findByUserId(1L) } returns null
        every { repository.save(any()) } answers {
            firstArg<BroadcastQualification>().apply { id = 1L }
        }

        val result = service.submit(1L, SubmitQualificationRequest(
            contactInfo = "13800138000",
            businessLicense = "BL123",
            personalInfo = "Test info"
        ))

        assertEquals(1L, result.id)
        assertEquals(1L, result.userId)
        assertEquals("13800138000", result.contactInfo)
        assertEquals(0, result.status)
    }

    @Test
    fun `should reject duplicate submission`() {
        every { repository.findByUserId(1L) } returns BroadcastQualification(
            id = 1L, userId = 1L, contactInfo = "old", status = 0
        )

        val ex = assertThrows<BusinessException> {
            service.submit(1L, SubmitQualificationRequest(contactInfo = "new"))
        }
        assertEquals(ErrorCode.QUALIFICATION_ALREADY_SUBMITTED, ex.code)
    }

    @Test
    fun `should approve qualification`() {
        val qualification = BroadcastQualification(
            id = 1L, userId = 1L, contactInfo = "13800138000", status = 0
        )
        every { repository.findById(1L) } returns Optional.of(qualification)
        every { repository.save(any()) } answers { firstArg() }

        val result = service.review(1L, 99L, ReviewQualificationRequest(status = 1))

        assertEquals(1, result.status)
        assertNotNull(result.reviewedAt)
        verify { repository.save(match { it.status == 1 && it.reviewedBy == 99L }) }
    }

    @Test
    fun `should reject qualification with reason`() {
        val qualification = BroadcastQualification(
            id = 1L, userId = 1L, contactInfo = "13800138000", status = 0
        )
        every { repository.findById(1L) } returns Optional.of(qualification)
        every { repository.save(any()) } answers { firstArg() }

        val result = service.review(1L, 99L, ReviewQualificationRequest(status = 2, rejectReason = "资料不全"))

        assertEquals(2, result.status)
        assertEquals("资料不全", result.rejectReason)
    }

    @Test
    fun `should return true when user is qualified`() {
        every { repository.findByUserId(1L) } returns BroadcastQualification(
            id = 1L, userId = 1L, contactInfo = "13800138000", status = 1
        )

        assertTrue(service.isQualified(1L))
    }

    @Test
    fun `should return false when user is not qualified`() {
        every { repository.findByUserId(1L) } returns null
        assertFalse(service.isQualified(1L))

        every { repository.findByUserId(2L) } returns BroadcastQualification(
            id = 2L, userId = 2L, contactInfo = "13800138000", status = 0
        )
        assertFalse(service.isQualified(2L))
    }
}
