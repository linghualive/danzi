package com.live.commerce.base.repository

import com.live.commerce.base.entity.MediaFile
import org.springframework.data.jpa.repository.JpaRepository

interface MediaFileRepository : JpaRepository<MediaFile, Long>
