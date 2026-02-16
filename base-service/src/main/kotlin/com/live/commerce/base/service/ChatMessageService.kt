package com.live.commerce.base.service

import com.live.commerce.base.dto.ChatMessageDTO
import com.live.commerce.base.websocket.MessageType
import com.live.commerce.common.dto.PageResult

interface ChatMessageService {
    fun saveMessage(roomId: Long, userId: Long, nickname: String, type: MessageType, content: String)
    fun getMessages(roomId: Long, page: Int, size: Int): PageResult<ChatMessageDTO>
}
