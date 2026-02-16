package com.live.commerce.base.controller

import com.live.commerce.base.dto.ChatMessageDTO
import com.live.commerce.base.service.ChatMessageService
import com.live.commerce.common.dto.PageResult
import com.live.commerce.common.dto.Result
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/live/room")
class ChatMessageController(
    private val chatMessageService: ChatMessageService
) {

    @GetMapping("/{roomId}/messages")
    fun getMessages(
        @PathVariable roomId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): Result<PageResult<ChatMessageDTO>> {
        return Result.ok(data = chatMessageService.getMessages(roomId, page, size))
    }
}
