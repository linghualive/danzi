package com.live.commerce.mall

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@ComponentScan(basePackages = ["com.live.commerce.mall", "com.live.commerce.common"])
@EnableFeignClients(basePackages = ["com.live.commerce.mall.feign"])
@EnableScheduling
class MallServiceApplication

fun main(args: Array<String>) {
    runApplication<MallServiceApplication>(*args)
}
