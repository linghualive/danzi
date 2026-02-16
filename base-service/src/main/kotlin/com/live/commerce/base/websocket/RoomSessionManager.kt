package com.live.commerce.base.websocket

import org.springframework.stereotype.Component
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

@Component
class RoomSessionManager {

    private val sessions = ConcurrentHashMap<Long, MutableSet<WebSocketSession>>()

    fun addSession(roomId: Long, session: WebSocketSession) {
        sessions.computeIfAbsent(roomId) { ConcurrentHashMap.newKeySet() }.add(session)
    }

    fun removeSession(roomId: Long, session: WebSocketSession) {
        sessions[roomId]?.remove(session)
    }

    fun broadcast(roomId: Long, message: String) {
        sessions[roomId]?.forEach { session ->
            if (session.isOpen) {
                session.sendMessage(TextMessage(message))
            }
        }
    }

    fun getSessionCount(roomId: Long): Int {
        return sessions[roomId]?.size ?: 0
    }
}
