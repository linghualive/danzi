package com.live.commerce.base.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class AdminUpdateUserRoleRequest(
    @field:Min(value = 0, message = "角色错误")
    @field:Max(value = 2, message = "角色错误")
    val role: Int = 0
)
