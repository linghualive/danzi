package com.live.commerce.base.controller

import cn.dev33.satoken.stp.StpUtil
import com.live.commerce.base.dto.BroadcastQualificationDTO
import com.live.commerce.base.dto.SubmitQualificationRequest
import com.live.commerce.base.service.BroadcastQualificationService
import com.live.commerce.common.dto.Result
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/broadcast/qualification")
class BroadcastQualificationController(
    private val qualificationService: BroadcastQualificationService
) {

    @PostMapping
    fun submit(@Valid @RequestBody request: SubmitQualificationRequest): Result<BroadcastQualificationDTO> {
        val userId = StpUtil.getLoginIdAsLong()
        return Result.ok(data = qualificationService.submit(userId, request))
    }

    @GetMapping("/me")
    fun getMyQualification(): Result<BroadcastQualificationDTO?> {
        val userId = StpUtil.getLoginIdAsLong()
        return Result.ok(data = qualificationService.getMyQualification(userId))
    }

    @GetMapping("/check")
    fun checkQualification(): Result<Boolean> {
        val userId = StpUtil.getLoginIdAsLong()
        return Result.ok(data = qualificationService.isQualified(userId))
    }
}
