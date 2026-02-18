package com.live.commerce.mall.controller

import cn.dev33.satoken.stp.StpUtil
import com.live.commerce.common.dto.Result
import com.live.commerce.mall.dto.CreateOrderRequest
import com.live.commerce.mall.dto.LiveSummaryDTO
import com.live.commerce.mall.dto.OrderDTO
import com.live.commerce.mall.dto.RefundRequest
import com.live.commerce.mall.service.OrderService
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

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
    fun getUserOrders(@RequestParam(required = false) keyword: String?): Result<List<OrderDTO>> {
        val userId = StpUtil.getLoginIdAsLong()
        val orders = orderService.getUserOrders(userId, keyword)
        return Result.ok(orders)
    }

    @GetMapping("/sold")
    fun getSoldOrders(@RequestParam(required = false) keyword: String?): Result<List<OrderDTO>> {
        val userId = StpUtil.getLoginIdAsLong()
        val orders = orderService.getSoldOrders(userId, keyword)
        return Result.ok(orders)
    }

    @GetMapping("/summary")
    fun getLiveSummary(
        @RequestParam sellerId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: LocalDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: LocalDateTime
    ): Result<LiveSummaryDTO> {
        return Result.ok(data = orderService.getLiveSummary(sellerId, from, to))
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

    @PutMapping("/{id}/refund-request")
    fun requestRefund(
        @PathVariable id: Long,
        @Valid @RequestBody request: RefundRequest
    ): Result<OrderDTO> {
        val userId = StpUtil.getLoginIdAsLong()
        val order = orderService.requestRefund(id, userId, request)
        return Result.ok(order)
    }

    @PutMapping("/{id}/refund-confirm")
    fun confirmRefund(@PathVariable id: Long): Result<OrderDTO> {
        val userId = StpUtil.getLoginIdAsLong()
        val order = orderService.confirmRefund(id, userId)
        return Result.ok(order)
    }
}
