package com.live.commerce.mall.controller

import cn.dev33.satoken.stp.StpUtil
import com.live.commerce.common.dto.Result
import com.live.commerce.mall.dto.CreateOrderRequest
import com.live.commerce.mall.dto.OrderDTO
import com.live.commerce.mall.service.OrderService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/order")
class OrderController(
    private val orderService: OrderService
) {

    @PostMapping
    fun createOrder(@Valid @RequestBody request: CreateOrderRequest): Result<OrderDTO> {
        val userId = StpUtil.getLoginIdAsLong()
        val order = orderService.createOrder(userId, request)
        return Result.ok(order)
    }

    @GetMapping("/{id}")
    fun getOrder(@PathVariable id: Long): Result<OrderDTO> {
        val userId = StpUtil.getLoginIdAsLong()
        val order = orderService.getOrder(id, userId)
        return Result.ok(order)
    }

    @GetMapping("/list")
    fun getUserOrders(): Result<List<OrderDTO>> {
        val userId = StpUtil.getLoginIdAsLong()
        val orders = orderService.getUserOrders(userId)
        return Result.ok(orders)
    }

    @PutMapping("/{id}/pay")
    fun payOrder(@PathVariable id: Long): Result<OrderDTO> {
        val userId = StpUtil.getLoginIdAsLong()
        val order = orderService.payOrder(id, userId)
        return Result.ok(order)
    }

    @PutMapping("/{id}/cancel")
    fun cancelOrder(@PathVariable id: Long): Result<OrderDTO> {
        val userId = StpUtil.getLoginIdAsLong()
        val order = orderService.cancelOrder(id, userId)
        return Result.ok(order)
    }
}
