package com.live.commerce.base.controller

import cn.dev33.satoken.stp.StpUtil
import com.live.commerce.base.dto.ConversationDTO
import com.live.commerce.base.dto.PrivateMessageDTO
import com.live.commerce.base.dto.SendPrivateMessageRequest
import com.live.commerce.base.service.UserSocialService
import com.live.commerce.common.dto.Result
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/user/message")
class PrivateMessageController(
    private val userSocialService: UserSocialService
) {
    @PostMapping("/send")
    fun send(@Valid @RequestBody request: SendPrivateMessageRequest): Result<PrivateMessageDTO> {
        val userId = StpUtil.getLoginIdAsLong()
        return Result.ok(data = userSocialService.sendMessage(userId, request))
    }

    @GetMapping("/conversation/{targetId}")
    fun conversation(@PathVariable targetId: Long): Result<List<PrivateMessageDTO>> {
        val userId = StpUtil.getLoginIdAsLong()
        return Result.ok(data = userSocialService.getConversation(userId, targetId))
    }

    @GetMapping("/conversations")
    fun conversations(): Result<List<ConversationDTO>> {
        val userId = StpUtil.getLoginIdAsLong()
        return Result.ok(data = userSocialService.getConversations(userId))
    }
}
