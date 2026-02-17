package com.live.commerce.base.support

import com.live.commerce.base.entity.User
import com.live.commerce.base.repository.UserRepository
import com.live.commerce.common.exception.BusinessException
import com.live.commerce.common.exception.ErrorCode
import org.springframework.stereotype.Component

@Component
class UserPermissionSupport(
    private val userRepository: UserRepository
) {
    fun getUserOrThrow(userId: Long): User {
        return userRepository.findById(userId)
            .orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
    }

    fun requireAdmin(userId: Long) {
        val user = getUserOrThrow(userId)
        if (user.role != 2) {
            throw BusinessException(ErrorCode.FORBIDDEN)
        }
    }
}
