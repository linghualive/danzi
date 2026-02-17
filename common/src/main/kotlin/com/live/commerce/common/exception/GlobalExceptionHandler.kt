package com.live.commerce.common.exception

import com.live.commerce.common.dto.Result
import org.slf4j.LoggerFactory
import org.springframework.validation.BindException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): Result<Nothing> {
        log.warn("Business exception: code={}, message={}", e.code, e.message)
        return Result.error(e.code, e.message)
    }

    @ExceptionHandler(BindException::class)
    fun handleBindException(e: BindException): Result<Nothing> {
        val message = e.fieldErrors.joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        log.warn("Validation exception: {}", message)
        return Result.error(ErrorCode.PARAM_ERROR, message)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException): Result<Nothing> {
        val message = e.bindingResult.fieldErrors.joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        log.warn("Method validation exception: {}", message)
        return Result.error(ErrorCode.PARAM_ERROR, message)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(e: IllegalArgumentException): Result<Nothing> {
        log.warn("Illegal argument: {}", e.message)
        return Result.error(ErrorCode.PARAM_ERROR, e.message ?: "参数错误")
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): Result<Nothing> {
        log.error("Unexpected exception", e)
        return Result.error(500, "服务器内部错误")
    }
}
