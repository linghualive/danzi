package com.live.commerce.base

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan(basePackages = ["com.live.commerce.base", "com.live.commerce.common"])
class BaseServiceApplication

fun main(args: Array<String>) {
    runApplication<BaseServiceApplication>(*args)
}
