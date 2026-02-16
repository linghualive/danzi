package com.live.commerce.base.controller

import com.live.commerce.base.dto.SrsCallbackRequest
import com.live.commerce.base.service.LiveCallbackService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/live/callback")
class LiveCallbackController(
    private val liveCallbackService: LiveCallbackService
) {

    @PostMapping("/on_publish")
    fun onPublish(@RequestBody request: SrsCallbackRequest): Map<String, Int> {
        liveCallbackService.onPublish(request)
        return mapOf("code" to 0)
    }

    @PostMapping("/on_unpublish")
    fun onUnpublish(@RequestBody request: SrsCallbackRequest): Map<String, Int> {
        liveCallbackService.onUnpublish(request)
        return mapOf("code" to 0)
    }
}
