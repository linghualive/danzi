package com.live.commerce.mall.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "media_file")
class MediaFile(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(nullable = false, length = 255)
    var fileName: String = "",

    @Column(nullable = false, length = 100)
    var contentType: String = "application/octet-stream",

    @Column(nullable = false)
    var fileSize: Long = 0,

    @Lob
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    var data: ByteArray = ByteArray(0),

    @Column(name = "created_by", nullable = false)
    var createdBy: Long = 0,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
)
