package com.live.commerce.base.service

import com.live.commerce.base.dto.SrsCallbackRequest

interface LiveCallbackService {
    fun onPublish(request: SrsCallbackRequest)
    fun onUnpublish(request: SrsCallbackRequest)
}
