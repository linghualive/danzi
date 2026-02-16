package com.live.commerce.mall

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class MallServiceApplicationTest : TestcontainersConfig() {

    @Test
    fun `should load application context`() {
        // Context loads successfully
    }
}
