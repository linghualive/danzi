package com.live.commerce.base

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class BaseServiceApplicationTest : TestcontainersConfig() {

    @Test
    fun `should load application context`() {
        // Context loads successfully
    }
}
