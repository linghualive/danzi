package com.live.commerce.common.dto

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ResultTest {

    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `should serialize ok result with data`() {
        val result = Result.ok<String>(data = "hello")
        val json = objectMapper.writeValueAsString(result)
        val tree = objectMapper.readTree(json)

        assertEquals(200, tree["code"].asInt())
        assertEquals("success", tree["message"].asText())
        assertEquals("hello", tree["data"].asText())
    }

    @Test
    fun `should serialize ok result without data`() {
        val result = Result.ok<String>()
        val json = objectMapper.writeValueAsString(result)
        val tree = objectMapper.readTree(json)

        assertEquals(200, tree["code"].asInt())
        assertEquals("success", tree["message"].asText())
        assertTrue(tree["data"].isNull)
    }

    @Test
    fun `should serialize error result`() {
        val result = Result.error<String>(400, "bad request")
        val json = objectMapper.writeValueAsString(result)
        val tree = objectMapper.readTree(json)

        assertEquals(400, tree["code"].asInt())
        assertEquals("bad request", tree["message"].asText())
        assertTrue(tree["data"].isNull)
    }
}
