package com.live.commerce.base.controller

import cn.dev33.satoken.stp.StpUtil
import com.live.commerce.base.dto.UpdateProfileRequest
import com.live.commerce.base.dto.UserProfileDTO
import com.live.commerce.base.service.UserSocialService
import com.live.commerce.common.dto.Result
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/user/profile")
class UserProfileController(
    private val userSocialService: UserSocialService
) {
    @GetMapping("/me")
    fun getMyProfile(): Result<UserProfileDTO> {
        val userId = StpUtil.getLoginIdAsLong()
        return Result.ok(data = userSocialService.getMyProfile(userId))
    }

    @PutMapping("/me")
    fun updateMyProfile(@RequestBody request: UpdateProfileRequest): Result<UserProfileDTO> {
        val userId = StpUtil.getLoginIdAsLong()
        return Result.ok(data = userSocialService.updateMyProfile(userId, request))
    }

    @GetMapping("/{userId}")
    fun getProfile(@PathVariable userId: Long): Result<UserProfileDTO> {
        return Result.ok(data = userSocialService.getProfile(userId))
    }
}
