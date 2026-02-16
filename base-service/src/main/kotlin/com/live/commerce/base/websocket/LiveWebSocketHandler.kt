package com.live.commerce.base.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import com.live.commerce.base.service.ChatMessageService
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

@Component
class LiveWebSocketHandler(
    private val roomSessionManager: RoomSessionManager,
    private val objectMapper: ObjectMapper,
    private val chatMessageService: ChatMessageService
) : TextWebSocketHandler() {

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val roomId = extractRoomId(session)
        roomSessionManager.addSession(roomId, session)
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val roomId = extractRoomId(session)
        val liveMessage = objectMapper.readValue(message.payload, LiveMessage::class.java)
        val broadcastJson = objectMapper.writeValueAsString(liveMessage)
        roomSessionManager.broadcast(roomId, broadcastJson)

        // Persist DANMAKU and COMMENT messages, skip LIKE
        if (liveMessage.type != MessageType.LIKE) {
            chatMessageService.saveMessage(
                roomId = roomId,
                userId = liveMessage.userId,
                nickname = liveMessage.nickname,
                type = liveMessage.type,
                content = liveMessage.content
            )
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        val roomId = extractRoomId(session)
        roomSessionManager.removeSession(roomId, session)
    }

    private fun extractRoomId(session: WebSocketSession): Long {
        val path = session.uri?.path ?: throw IllegalArgumentException("No URI in session")
        // Path format: /ws/live/{roomId}
        return path.substringAfterLast("/").toLong()
    }
}
