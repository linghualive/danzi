package com.live.commerce.mall.schedule

import com.live.commerce.mall.service.OrderService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class OrderExpireSchedule(
    private val orderService: OrderService
) {
    @Scheduled(fixedDelay = 60000)
    fun expirePendingOrders() {
        orderService.autoCancelExpiredOrders()
    }
}
