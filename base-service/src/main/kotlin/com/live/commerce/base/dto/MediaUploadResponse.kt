package com.live.commerce.base.dto

data class MediaUploadResponse(
    val id: Long,
    val fileName: String,
    val fileSize: Long,
    val contentType: String,
    val url: String
)
