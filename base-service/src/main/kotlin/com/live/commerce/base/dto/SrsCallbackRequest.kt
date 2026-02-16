package com.live.commerce.base.dto

data class SrsCallbackRequest(
    val action: String = "",    // on_publish / on_unpublish
    val ip: String = "",
    val vhost: String = "",
    val app: String = "",
    val stream: String = "",    // streamKey
    val param: String = ""
)
