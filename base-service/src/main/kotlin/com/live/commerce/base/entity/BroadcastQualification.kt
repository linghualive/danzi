package com.live.commerce.base.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "broadcast_qualification")
class BroadcastQualification(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "user_id", nullable = false, unique = true)
    var userId: Long = 0,

    @Column(name = "contact_info", nullable = false)
    var contactInfo: String = "",

    @Column(name = "business_license")
    var businessLicense: String? = null,

    @Column(name = "personal_info", length = 1000)
    var personalInfo: String? = null,

    @Column(nullable = false)
    var status: Int = 0, // 0=pending, 1=approved, 2=rejected

    @Column(name = "reject_reason")
    var rejectReason: String? = null,

    @Column(name = "reviewed_by")
    var reviewedBy: Long? = null,

    @Column(name = "reviewed_at")
    var reviewedAt: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
