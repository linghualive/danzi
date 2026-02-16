package com.live.commerce.mall.feign

import feign.RequestInterceptor
import feign.RequestTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Configuration
class FeignConfig {

    @Bean
    fun satokenRequestInterceptor(): RequestInterceptor {
        return RequestInterceptor { template: RequestTemplate ->
            val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
            val request = attributes?.request
            val satoken = request?.getHeader("satoken")
            if (satoken != null) {
                template.header("satoken", satoken)
            }
        }
    }
}
