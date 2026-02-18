package com.live.commerce.base.controller

import cn.dev33.satoken.stp.StpUtil
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.live.commerce.base.dto.*
import com.live.commerce.base.entity.RoomWarning
import com.live.commerce.base.repository.LiveRoomRepository
import com.live.commerce.base.repository.RoomWarningRepository
import com.live.commerce.base.repository.UserRepository
import com.live.commerce.base.service.BroadcastQualificationService
import com.live.commerce.base.service.LiveRoomService
import com.live.commerce.base.support.UserPermissionSupport
import com.live.commerce.base.websocket.RoomSessionManager
import com.live.commerce.common.dto.PageResult
import com.live.commerce.common.dto.Result
import com.live.commerce.common.exception.BusinessException
import com.live.commerce.common.exception.ErrorCode
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin")
class AdminController(
    private val userRepository: UserRepository,
    private val liveRoomRepository: LiveRoomRepository,
    private val roomWarningRepository: RoomWarningRepository,
    private val liveRoomService: LiveRoomService,
    private val userPermissionSupport: UserPermissionSupport,
    private val broadcastQualificationService: BroadcastQualificationService,
    private val roomSessionManager: RoomSessionManager
) {
    private val objectMapper = jacksonObjectMapper()

    @GetMapping("/users")
    fun users(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) keyword: String?
    ): Result<PageResult<AdminUserDTO>> {
        val adminId = StpUtil.getLoginIdAsLong()
        userPermissionSupport.requireAdmin(adminId)
        val userPage = userRepository.search(keyword?.trim()?.ifBlank { null }, PageRequest.of(page, size))
        return Result.ok(
            data = PageResult(
                content = userPage.content.map {
                    AdminUserDTO(
                        id = it.id,
                        username = it.username,
                        nickname = it.nickname,
                        role = it.role,
                        status = it.status,
                        createdAt = it.createdAt
                    )
                },
                page = userPage.number,
                size = userPage.size,
                totalElements = userPage.totalElements,
                totalPages = userPage.totalPages
            )
        )
    }

    @PutMapping("/user/{id}/role")
    fun updateUserRole(
        @PathVariable id: Long,
        @Valid @RequestBody request: AdminUpdateUserRoleRequest
    ): Result<Nothing> {
        val adminId = StpUtil.getLoginIdAsLong()
        userPermissionSupport.requireAdmin(adminId)
        val user = userRepository.findById(id)
            .orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
        user.role = request.role
        userRepository.save(user)
        return Result.ok()
    }

    @PutMapping("/user/{id}/status")
    fun updateUserStatus(
        @PathVariable id: Long,
        @Valid @RequestBody request: AdminUpdateUserStatusRequest
    ): Result<Nothing> {
        val adminId = StpUtil.getLoginIdAsLong()
        userPermissionSupport.requireAdmin(adminId)
        val user = userRepository.findById(id)
            .orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
        user.status = request.status
        userRepository.save(user)
        return Result.ok()
    }

    @GetMapping("/rooms")
    fun rooms(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) keyword: String?
    ): Result<PageResult<AdminRoomDTO>> {
        val adminId = StpUtil.getLoginIdAsLong()
        userPermissionSupport.requireAdmin(adminId)
        val roomPage = liveRoomRepository.searchAllRooms(keyword?.trim()?.ifBlank { null }, PageRequest.of(page, size))
        val ownerIds = roomPage.content.map { it.userId }.distinct()
        val ownerMap = userRepository.findAllById(ownerIds).associateBy { it.id }
        return Result.ok(
            data = PageResult(
                content = roomPage.content.map {
                    AdminRoomDTO(
                        id = it.id,
                        userId = it.userId,
                        title = it.title,
                        status = it.status,
                        closedReason = it.closedReason,
                        ownerNickname = ownerMap[it.userId]?.nickname,
                        updatedAt = it.updatedAt
                    )
                },
                page = roomPage.number,
                size = roomPage.size,
                totalElements = roomPage.totalElements,
                totalPages = roomPage.totalPages
            )
        )
    }

    @PostMapping("/room/{id}/warn")
    fun warnRoom(
        @PathVariable id: Long,
        @Valid @RequestBody request: AdminWarnRoomRequest
    ): Result<Nothing> {
        val adminId = StpUtil.getLoginIdAsLong()
        userPermissionSupport.requireAdmin(adminId)
        liveRoomRepository.findById(id).orElseThrow { BusinessException(ErrorCode.ROOM_NOT_FOUND) }
        val warning = roomWarningRepository.save(
            RoomWarning(
                roomId = id,
                adminId = adminId,
                message = request.message
            )
        )
        val payload = mapOf(
            "id" to warning.id,
            "type" to "WARNING",
            "userId" to adminId,
            "nickname" to "系统管理员",
            "content" to warning.message,
            "roomId" to id,
            "timestamp" to System.currentTimeMillis()
        )
        runCatching {
            roomSessionManager.broadcast(id, objectMapper.writeValueAsString(payload))
        }
        return Result.ok()
    }

    @PutMapping("/room/{id}/close")
    fun closeRoom(
        @PathVariable id: Long,
        @Valid @RequestBody request: AdminCloseRoomRequest
    ): Result<LiveRoomDTO> {
        val adminId = StpUtil.getLoginIdAsLong()
        userPermissionSupport.requireAdmin(adminId)
        return Result.ok(data = liveRoomService.adminCloseRoom(id, adminId, request.reason))
    }

    @GetMapping("/qualifications")
    fun getQualifications(): Result<List<BroadcastQualificationDTO>> {
        val adminId = StpUtil.getLoginIdAsLong()
        userPermissionSupport.requireAdmin(adminId)
        return Result.ok(data = broadcastQualificationService.getPendingApplications())
    }

    @PutMapping("/qualification/{id}/review")
    fun reviewQualification(
        @PathVariable id: Long,
        @Valid @RequestBody request: ReviewQualificationRequest
    ): Result<BroadcastQualificationDTO> {
        val adminId = StpUtil.getLoginIdAsLong()
        userPermissionSupport.requireAdmin(adminId)
        return Result.ok(data = broadcastQualificationService.review(id, adminId, request))
    }
}
