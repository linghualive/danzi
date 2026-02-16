package com.live.commerce.common.exception

import com.live.commerce.common.dto.Result
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.validation.BindException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    @Test
    fun `should handle BusinessException and return error result`() {
        val exception = BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在")
        val result = handler.handleBusinessException(exception)

        assertEquals(ErrorCode.USER_NOT_FOUND, result.code)
        assertEquals("用户不存在", result.message)
        assertNull(result.data)
    }

    @Test
    fun `should handle generic Exception and return 500 error`() {
        val exception = RuntimeException("unexpected error")
        val result = handler.handleException(exception)

        assertEquals(500, result.code)
        assertEquals("服务器内部错误", result.message)
        assertNull(result.data)
    }

    @Test
    fun `should handle BindException and return validation error`() {
        val bindException = BindException(Any(), "target")
        bindException.addError(FieldError("target", "username", "用户名不能为空"))
        val result = handler.handleBindException(bindException)

        assertEquals(ErrorCode.PARAM_ERROR, result.code)
        assertTrue(result.message.contains("用户名不能为空"))
    }

    @Test
    fun `should handle IllegalArgumentException and return param error`() {
        val exception = IllegalArgumentException("参数不合法")
        val result = handler.handleIllegalArgumentException(exception)

        assertEquals(ErrorCode.PARAM_ERROR, result.code)
        assertEquals("参数不合法", result.message)
    }
}
