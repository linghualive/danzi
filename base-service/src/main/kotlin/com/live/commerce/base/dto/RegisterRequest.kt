package com.live.commerce.base.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:NotBlank(message = "用户名不能为空")
    @field:Size(min = 3, max = 50, message = "用户名长度3-50")
    val username: String = "",

    @field:NotBlank(message = "密码不能为空")
    @field:Size(min = 6, max = 100, message = "密码长度6-100")
    val password: String = "",

    @field:NotBlank(message = "昵称不能为空")
    val nickname: String = ""
)
