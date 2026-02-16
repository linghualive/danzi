package com.live.commerce.common.exception

class BusinessException(
    val code: Int,
    override val message: String = ErrorCode.getMessage(code)
) : RuntimeException(message)
