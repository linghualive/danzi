package com.live.commerce.base.service

import com.live.commerce.base.dto.LoginRequest
import com.live.commerce.base.dto.LoginResponse
import com.live.commerce.base.dto.RegisterRequest
import com.live.commerce.common.dto.UserDTO

interface UserService {
    fun register(request: RegisterRequest): UserDTO
    fun login(request: LoginRequest): LoginResponse
    fun getUserById(id: Long): UserDTO
}
