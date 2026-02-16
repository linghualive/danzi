package com.live.commerce.common.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicLong

object OrderNoGenerator {

    private val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    private val sequence = AtomicLong(0)

    fun generate(): String {
        val timestamp = LocalDateTime.now().format(formatter)
        val seq = sequence.incrementAndGet() % 10000
        return "$timestamp${String.format("%04d", seq)}"
    }
}
