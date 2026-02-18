package com.live.commerce.common.exception

object ErrorCode {
    const val PARAM_ERROR = 400
    const val UNAUTHORIZED = 401
    const val FORBIDDEN = 403

    const val USER_NOT_FOUND = 1001
    const val USERNAME_ALREADY_EXISTS = 1002
    const val PASSWORD_INCORRECT = 1003
    const val USER_ALREADY_FOLLOWED = 1004
    const val CANNOT_FOLLOW_SELF = 1005

    const val ROOM_NOT_FOUND = 2001
    const val ROOM_STATUS_ERROR = 2002
    const val ROOM_PERMISSION_DENIED = 2003
    const val ROOM_ALREADY_EXISTS = 2004
    const val ROOM_CLOSED_BY_ADMIN = 2005

    const val PRODUCT_NOT_FOUND = 3001
    const val STOCK_NOT_ENOUGH = 3002
    const val PRODUCT_PERMISSION_DENIED = 3003

    const val ORDER_NOT_FOUND = 4001
    const val ORDER_STATUS_ERROR = 4002
    const val ORDER_EXPIRED = 4003
    const val REFUND_REASON_REQUIRED = 4004

    const val MEDIA_NOT_FOUND = 5001

    const val USER_DISABLED = 1006

    const val QUALIFICATION_ALREADY_SUBMITTED = 6001
    const val QUALIFICATION_NOT_FOUND = 6002
    const val QUALIFICATION_NOT_APPROVED = 6003

    private val messages = mapOf(
        PARAM_ERROR to "参数错误",
        UNAUTHORIZED to "未登录",
        FORBIDDEN to "无权限",
        USER_NOT_FOUND to "用户不存在",
        USERNAME_ALREADY_EXISTS to "用户名已存在",
        PASSWORD_INCORRECT to "密码错误",
        USER_ALREADY_FOLLOWED to "已关注该用户",
        CANNOT_FOLLOW_SELF to "不能关注自己",
        ROOM_NOT_FOUND to "直播间不存在",
        ROOM_STATUS_ERROR to "直播间状态错误",
        ROOM_PERMISSION_DENIED to "无直播间操作权限",
        ROOM_ALREADY_EXISTS to "每个用户只能创建一个直播间",
        ROOM_CLOSED_BY_ADMIN to "直播间已被管理员关闭",
        PRODUCT_NOT_FOUND to "商品不存在",
        STOCK_NOT_ENOUGH to "库存不足",
        PRODUCT_PERMISSION_DENIED to "无商品操作权限",
        ORDER_NOT_FOUND to "订单不存在",
        ORDER_STATUS_ERROR to "订单状态错误",
        ORDER_EXPIRED to "订单已超时",
        REFUND_REASON_REQUIRED to "退款原因不能为空",
        MEDIA_NOT_FOUND to "文件不存在",
        USER_DISABLED to "账号已被禁用",
        QUALIFICATION_ALREADY_SUBMITTED to "资质申请已提交",
        QUALIFICATION_NOT_FOUND to "资质申请不存在",
        QUALIFICATION_NOT_APPROVED to "未通过开播资格审核，请先在个人主页提交开播资格申请"
    )

    fun getMessage(code: Int): String = messages[code] ?: "未知错误"
}
