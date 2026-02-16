package com.live.commerce.base.service.impl

import com.live.commerce.base.dto.ChatMessageDTO
import com.live.commerce.base.entity.ChatMessage
import com.live.commerce.base.repository.ChatMessageRepository
import com.live.commerce.base.service.ChatMessageService
import com.live.commerce.base.websocket.MessageType
import com.live.commerce.common.dto.PageResult
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ChatMessageServiceImpl(
    private val chatMessageRepository: ChatMessageRepository
) : ChatMessageService {

    @Transactional
    override fun saveMessage(roomId: Long, userId: Long, nickname: String, type: MessageType, content: String) {
        val chatMessage = ChatMessage(
            roomId = roomId,
            userId = userId,
            nickname = nickname,
            type = type.name,
            content = content
        )
        chatMessageRepository.save(chatMessage)
    }

    override fun getMessages(roomId: Long, page: Int, size: Int): PageResult<ChatMessageDTO> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val pageResult = chatMessageRepository.findByRoomId(roomId, pageable)
        return PageResult(
            content = pageResult.content.map { toDTO(it) },
            page = pageResult.number,
            size = pageResult.size,
            totalElements = pageResult.totalElements,
            totalPages = pageResult.totalPages
        )
    }

    private fun toDTO(entity: ChatMessage): ChatMessageDTO = ChatMessageDTO(
        id = entity.id,
        roomId = entity.roomId,
        userId = entity.userId,
        nickname = entity.nickname,
        type = entity.type,
        content = entity.content,
        createdAt = entity.createdAt
    )
}
