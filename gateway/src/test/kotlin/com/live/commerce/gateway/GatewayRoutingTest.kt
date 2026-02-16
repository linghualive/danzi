package com.live.commerce.gateway

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class GatewayRoutingTest {

    @Autowired
    lateinit var webTestClient: WebTestClient

    companion object {
        @Container
        @JvmStatic
        val redis = GenericContainer("redis:7").apply {
            withExposedPorts(6379)
        }

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.redis.host") { redis.host }
            registry.add("spring.data.redis.port") { redis.getMappedPort(6379) }
        }
    }

    @Test
    fun `should reject protected route when no token provided`() {
        webTestClient.get()
            .uri("/api/user/1")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.code").isEqualTo(401)
    }

    @Test
    fun `should allow access to register without token`() {
        webTestClient.post()
            .uri("/api/user/register")
            .exchange()
            .expectStatus().value { status ->
                // Should not get auth error - will get 502 (no downstream) or something else
                // but the response body should NOT contain auth error
            }
            .expectBody()
            .consumeWith { result ->
                val body = result.responseBody?.let { String(it) } ?: ""
                assert(!body.contains("\"code\":401")) {
                    "Register should not require authentication"
                }
            }
    }

    @Test
    fun `should allow access to login without token`() {
        webTestClient.post()
            .uri("/api/user/login")
            .exchange()
            .expectBody()
            .consumeWith { result ->
                val body = result.responseBody?.let { String(it) } ?: ""
                assert(!body.contains("\"code\":401")) {
                    "Login should not require authentication"
                }
            }
    }

    @Test
    fun `should allow access to SRS callback without token`() {
        webTestClient.post()
            .uri("/api/live/callback/on_publish")
            .exchange()
            .expectBody()
            .consumeWith { result ->
                val body = result.responseBody?.let { String(it) } ?: ""
                assert(!body.contains("\"code\":401")) {
                    "SRS callback should not require authentication"
                }
            }
    }

    @Test
    fun `should reject product API without token`() {
        webTestClient.get()
            .uri("/api/product/1")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.code").isEqualTo(401)
    }

    @Test
    fun `should reject order API without token`() {
        webTestClient.get()
            .uri("/api/order/1")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.code").isEqualTo(401)
    }

    @Test
    fun `should reject live room creation without token`() {
        webTestClient.post()
            .uri("/api/live/room")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.code").isEqualTo(401)
    }

    @Test
    fun `should have base service user route configured`() {
        // Route exists, returns auth error (not 404)
        webTestClient.get()
            .uri("/api/user/1")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.code").isEqualTo(401)
    }

    @Test
    fun `should have mall service product route configured`() {
        webTestClient.get()
            .uri("/api/product/list")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.code").isEqualTo(401)
    }
}
