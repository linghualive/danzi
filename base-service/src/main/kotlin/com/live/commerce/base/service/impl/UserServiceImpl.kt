package com.live.commerce.base.service.impl

import cn.dev33.satoken.stp.StpUtil
import com.live.commerce.base.dto.LoginRequest
import com.live.commerce.base.dto.LoginResponse
import com.live.commerce.base.dto.RegisterRequest
import com.live.commerce.base.entity.User
import com.live.commerce.base.repository.UserRepository
import com.live.commerce.base.service.UserService
import com.live.commerce.common.dto.UserDTO
import com.live.commerce.common.exception.BusinessException
import com.live.commerce.common.exception.ErrorCode
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) : UserService {

    @Transactional
    override fun register(request: RegisterRequest): UserDTO {
        userRepository.findByUsername(request.username)?.let {
            throw BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS)
        }

        val user = User(
            username = request.username,
            password = passwordEncoder.encode(request.password),
            nickname = request.nickname
        )
        val saved = userRepository.save(user)
        return toDTO(saved)
    }

    override fun login(request: LoginRequest): LoginResponse {
        val user = userRepository.findByUsername(request.username)
            ?: throw BusinessException(ErrorCode.USER_NOT_FOUND)

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw BusinessException(ErrorCode.PASSWORD_INCORRECT)
        }

        StpUtil.login(user.id)
        val token = StpUtil.getTokenValue()

        return LoginResponse(
            token = token,
            userId = user.id,
            username = user.username,
            nickname = user.nickname,
            role = user.role
        )
    }

    override fun getUserById(id: Long): UserDTO {
        val user = userRepository.findById(id)
            .orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
        return toDTO(user)
    }

    private fun toDTO(user: User): UserDTO = UserDTO(
        id = user.id,
        username = user.username,
        nickname = user.nickname,
        avatar = user.avatar,
        role = user.role
    )
}
