package com.live.commerce.base.repository

import com.live.commerce.base.entity.ChatMessage
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ChatMessageRepository : JpaRepository<ChatMessage, Long> {
    fun findByRoomId(roomId: Long, pageable: Pageable): Page<ChatMessage>
}
