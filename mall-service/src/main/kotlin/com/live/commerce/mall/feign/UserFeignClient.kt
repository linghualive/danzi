package com.live.commerce.mall.feign

import com.live.commerce.common.dto.Result
import com.live.commerce.common.dto.UserDTO
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@FeignClient(
    name = "base-service",
    url = "\${feign.base-service.url:http://localhost:9001}",
    configuration = [FeignConfig::class]
)
interface UserFeignClient {

    @GetMapping("/api/user/{id}")
    fun getUserById(@PathVariable id: Long): Result<UserDTO>
}
