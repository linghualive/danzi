package com.live.commerce.base.controller

import cn.dev33.satoken.stp.StpUtil
import com.live.commerce.base.dto.FollowStatsDTO
import com.live.commerce.base.service.UserSocialService
import com.live.commerce.common.dto.Result
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/user/follow")
class FollowController(
    private val userSocialService: UserSocialService
) {

    @PostMapping("/{targetId}")
    fun follow(@PathVariable targetId: Long): Result<Nothing> {
        val userId = StpUtil.getLoginIdAsLong()
        userSocialService.follow(userId, targetId)
        return Result.ok()
    }

    @DeleteMapping("/{targetId}")
    fun unfollow(@PathVariable targetId: Long): Result<Nothing> {
        val userId = StpUtil.getLoginIdAsLong()
        userSocialService.unfollow(userId, targetId)
        return Result.ok()
    }

    @GetMapping("/{targetId}/stats")
    fun stats(@PathVariable targetId: Long): Result<FollowStatsDTO> {
        val userId = StpUtil.getLoginIdAsLong()
        return Result.ok(data = userSocialService.getFollowStats(userId, targetId))
    }
}
