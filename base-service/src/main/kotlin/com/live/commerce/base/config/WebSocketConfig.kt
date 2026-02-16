package com.live.commerce.base.config

import com.live.commerce.base.websocket.LiveWebSocketHandler
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val liveWebSocketHandler: LiveWebSocketHandler
) : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(liveWebSocketHandler, "/ws/live/{roomId}")
            .setAllowedOrigins("*")
    }
}
