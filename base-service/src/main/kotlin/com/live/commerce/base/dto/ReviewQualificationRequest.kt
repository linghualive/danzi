package com.live.commerce.base.dto

data class ReviewQualificationRequest(
    val status: Int, // 1=approved, 2=rejected
    val rejectReason: String? = null
)
