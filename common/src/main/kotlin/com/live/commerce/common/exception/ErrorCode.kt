package com.live.commerce.common.exception

object ErrorCode {
    const val PARAM_ERROR = 400
    const val UNAUTHORIZED = 401
    const val FORBIDDEN = 403
    const val USER_NOT_FOUND = 1001
    const val USERNAME_ALREADY_EXISTS = 1002
    const val PASSWORD_INCORRECT = 1003
    const val ROOM_NOT_FOUND = 2001
    const val ROOM_STATUS_ERROR = 2002
    const val ROOM_PERMISSION_DENIED = 2003
    const val PRODUCT_NOT_FOUND = 3001
    const val STOCK_NOT_ENOUGH = 3002
    const val ORDER_NOT_FOUND = 4001
    const val ORDER_STATUS_ERROR = 4002

    private val messages = mapOf(
        PARAM_ERROR to "参数错误",
        UNAUTHORIZED to "未登录",
        FORBIDDEN to "无权限",
        USER_NOT_FOUND to "用户不存在",
        USERNAME_ALREADY_EXISTS to "用户名已存在",
        PASSWORD_INCORRECT to "密码错误",
        ROOM_NOT_FOUND to "直播间不存在",
        ROOM_STATUS_ERROR to "直播间状态错误",
        ROOM_PERMISSION_DENIED to "无直播间操作权限",
        PRODUCT_NOT_FOUND to "商品不存在",
        STOCK_NOT_ENOUGH to "库存不足",
        ORDER_NOT_FOUND to "订单不存在",
        ORDER_STATUS_ERROR to "订单状态错误"
    )

    fun getMessage(code: Int): String = messages[code] ?: "未知错误"
}
