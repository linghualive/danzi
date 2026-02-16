package com.live.commerce.common.dto

data class Result<T>(
    val code: Int,
    val message: String,
    val data: T? = null
) {
    companion object {
        fun <T> ok(data: T? = null): Result<T> = Result(200, "success", data)

        fun <T> ok(message: String, data: T? = null): Result<T> = Result(200, message, data)

        fun <T> error(code: Int, message: String): Result<T> = Result(code, message, null)

        fun <T> error(message: String): Result<T> = Result(500, message, null)
    }
}
