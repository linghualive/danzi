package com.live.commerce.gateway.config

import cn.dev33.satoken.`fun`.SaFunction
import cn.dev33.satoken.exception.NotLoginException
import cn.dev33.satoken.reactor.filter.SaReactorFilter
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
                "/api/live/callback/**"
            )
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
