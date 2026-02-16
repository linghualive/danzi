package com.live.commerce.base.controller

import com.live.commerce.base.dto.LoginRequest
import com.live.commerce.base.dto.LoginResponse
import com.live.commerce.base.dto.RegisterRequest
import com.live.commerce.base.service.UserService
import com.live.commerce.common.dto.Result
import com.live.commerce.common.dto.UserDTO
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/user")
class UserController(
    private val userService: UserService
) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): Result<UserDTO> {
        return Result.ok(data = userService.register(request))
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): Result<LoginResponse> {
        return Result.ok(data = userService.login(request))
    }

    @GetMapping("/{id}")
    fun getUserById(@PathVariable id: Long): Result<UserDTO> {
        return Result.ok(data = userService.getUserById(id))
    }
}
