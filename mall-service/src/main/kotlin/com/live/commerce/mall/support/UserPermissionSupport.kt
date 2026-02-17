package com.live.commerce.mall.support

import com.live.commerce.common.dto.UserDTO
import com.live.commerce.common.exception.BusinessException
import com.live.commerce.common.exception.ErrorCode
import com.live.commerce.mall.feign.UserFeignClient
import org.springframework.stereotype.Component

@Component
class UserPermissionSupport(
    private val userFeignClient: UserFeignClient
) {
    fun getUser(userId: Long): UserDTO {
        val result = userFeignClient.getUserById(userId)
        return result.data ?: throw BusinessException(ErrorCode.USER_NOT_FOUND)
    }

    fun ensureProductOperator(operatorId: Long, sellerId: Long) {
        val user = getUser(operatorId)
        val isAdmin = user.role == 2
        if (!isAdmin && operatorId != sellerId) {
            throw BusinessException(ErrorCode.PRODUCT_PERMISSION_DENIED)
        }
    }

    fun ensureOrderAccessor(operatorId: Long, buyerId: Long, sellerId: Long) {
        val user = getUser(operatorId)
        val isAdmin = user.role == 2
        if (!isAdmin && operatorId != buyerId && operatorId != sellerId) {
            throw BusinessException(ErrorCode.FORBIDDEN)
        }
    }
}
