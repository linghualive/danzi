package com.live.commerce.base.dto

import jakarta.validation.constraints.NotBlank

data class SubmitQualificationRequest(
    @field:NotBlank(message = "联系方式不能为空")
    val contactInfo: String = "",
    val businessLicense: String? = null,
    val personalInfo: String? = null
)
