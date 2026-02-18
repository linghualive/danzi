package com.live.commerce.gateway.config

import cn.dev33.satoken.`fun`.SaFunction
import cn.dev33.satoken.exception.NotLoginException
import cn.dev33.satoken.reactor.filter.SaReactorFilter
import cn.dev33.satoken.router.SaHttpMethod
import cn.dev33.satoken.router.SaRouter
import cn.dev33.satoken.stp.StpUtil
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus

@Configuration
class SaTokenGatewayConfig {

    private val objectMapper = jacksonObjectMapper()

    @Bean
    fun saReactorFilter(): SaReactorFilter {
        return SaReactorFilter()
            .addInclude("/**")
            .addExclude(
                "/api/user/register",
                "/api/user/login",
                "/api/user/{id}",
                "/api/user/profile/{userId}",
                "/api/live/room/{id}",
                "/api/live/room/list",
                "/api/live/callback/**",
                "/api/live/room/*/messages",
                "/api/base/media/public/**",
                "/api/product/media/public/**",
                "/ws/**"
            )
            .setBeforeAuth {
                // 放行 CORS 预检请求
                SaRouter.match(SaHttpMethod.OPTIONS).stop()
            }
            .setAuth {
                SaRouter.match("/**")
                    .check(SaFunction { StpUtil.checkLogin() })
            }
            .setError { e ->
                val response = mapOf(
                    "code" to HttpStatus.UNAUTHORIZED.value(),
                    "message" to (e.message ?: "未登录")
                )
                objectMapper.writeValueAsString(response)
            }
    }
}
