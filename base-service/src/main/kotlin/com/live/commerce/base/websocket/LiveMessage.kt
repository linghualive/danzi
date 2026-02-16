package com.live.commerce.base.websocket

data class LiveMessage(
    val type: MessageType,
    val userId: Long,
    val nickname: String,
    val content: String,
    val roomId: Long,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageType {
    DANMAKU, COMMENT, LIKE
}
