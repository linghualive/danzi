package com.live.commerce.base.controller

import cn.dev33.satoken.stp.StpUtil
import com.live.commerce.base.dto.MediaUploadResponse
import com.live.commerce.base.entity.MediaFile
import com.live.commerce.base.repository.MediaFileRepository
import com.live.commerce.common.dto.Result
import com.live.commerce.common.exception.BusinessException
import com.live.commerce.common.exception.ErrorCode
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/base/media")
class MediaController(
    private val mediaFileRepository: MediaFileRepository
) {

    @PostMapping("/upload")
    fun upload(@RequestParam("file") file: MultipartFile): Result<MediaUploadResponse> {
        if (file.isEmpty) {
            throw BusinessException(ErrorCode.PARAM_ERROR, "上传文件不能为空")
        }

        val saved = mediaFileRepository.save(
            MediaFile(
                fileName = file.originalFilename ?: "file",
                contentType = file.contentType ?: "application/octet-stream",
                fileSize = file.size,
                data = file.bytes,
                createdBy = StpUtil.getLoginIdAsLong()
            )
        )

        return Result.ok(
            data = MediaUploadResponse(
                id = saved.id,
                fileName = saved.fileName,
                fileSize = saved.fileSize,
                contentType = saved.contentType,
                url = "/api/base/media/public/${saved.id}"
            )
        )
    }

    @GetMapping("/public/{id}")
    fun get(@PathVariable id: Long): ResponseEntity<ByteArray> {
        val file = mediaFileRepository.findById(id)
            .orElseThrow { BusinessException(ErrorCode.MEDIA_NOT_FOUND) }

        val headers = HttpHeaders()
        headers.contentType = MediaType.parseMediaType(file.contentType)
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"${file.fileName}\"")
        return ResponseEntity.ok()
            .headers(headers)
            .body(file.data)
    }
}
