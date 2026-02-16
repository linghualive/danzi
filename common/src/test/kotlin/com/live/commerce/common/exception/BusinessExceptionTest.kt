package com.live.commerce.common.exception

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BusinessExceptionTest {

    @Test
    fun `should create business exception with code and message`() {
        val exception = BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在")

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.code)
        assertEquals("用户不存在", exception.message)
    }

    @Test
    fun `should create business exception with default message from error code`() {
        val exception = BusinessException(ErrorCode.PARAM_ERROR)

        assertEquals(ErrorCode.PARAM_ERROR, exception.code)
        assertEquals("参数错误", exception.message)
    }
}
