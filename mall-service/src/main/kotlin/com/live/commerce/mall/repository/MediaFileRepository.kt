package com.live.commerce.mall.repository

import com.live.commerce.mall.entity.MediaFile
import org.springframework.data.jpa.repository.JpaRepository

interface MediaFileRepository : JpaRepository<MediaFile, Long>
