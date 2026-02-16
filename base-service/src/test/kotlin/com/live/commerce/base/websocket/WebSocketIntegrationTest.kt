package com.live.commerce.base.websocket

import com.live.commerce.base.TestcontainersConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketHttpHeaders
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.net.URI
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class WebSocketIntegrationTest : TestcontainersConfig() {

    @LocalServerPort
    private var port: Int = 0

    @Test
    fun `should connect to websocket endpoint`() {
        val client = StandardWebSocketClient()
        val uri = URI("ws://localhost:$port/ws/live/1")

        val future = client.execute(object : TextWebSocketHandler() {}, WebSocketHttpHeaders(), uri)
        val session = future.get(5, TimeUnit.SECONDS)

        assertTrue(session.isOpen)
        session.close()
    }

    @Test
    fun `should receive broadcast message`() {
        val client = StandardWebSocketClient()
        val uri = URI("ws://localhost:$port/ws/live/100")

        val receivedLatch = CountDownLatch(1)
        var receivedMessage: String? = null

        // Connect first client
        val session1 = client.execute(object : TextWebSocketHandler() {
            override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
                receivedMessage = message.payload
                receivedLatch.countDown()
            }
        }, WebSocketHttpHeaders(), uri).get(5, TimeUnit.SECONDS)

        // Connect second client
        val session2 = client.execute(object : TextWebSocketHandler() {}, WebSocketHttpHeaders(), uri)
            .get(5, TimeUnit.SECONDS)

        // Small delay to ensure both sessions are registered
        Thread.sleep(200)

        // Send message from session2
        val liveMessage = """{"type":"DANMAKU","userId":1,"nickname":"user1","content":"Hello!","roomId":100,"timestamp":${System.currentTimeMillis()}}"""
        session2.sendMessage(TextMessage(liveMessage))

        // Wait for message to be received
        val received = receivedLatch.await(5, TimeUnit.SECONDS)
        assertTrue(received, "Should have received a broadcast message")
        assertNotNull(receivedMessage)
        assertTrue(receivedMessage!!.contains("Hello!"))

        session1.close()
        session2.close()
    }

    @Test
    fun `should handle disconnection`() {
        val client = StandardWebSocketClient()
        val uri = URI("ws://localhost:$port/ws/live/200")

        val session = client.execute(object : TextWebSocketHandler() {}, WebSocketHttpHeaders(), uri)
            .get(5, TimeUnit.SECONDS)

        assertTrue(session.isOpen)
        session.close()

        // Small delay to allow close to propagate
        Thread.sleep(200)

        assertFalse(session.isOpen)
    }
}
