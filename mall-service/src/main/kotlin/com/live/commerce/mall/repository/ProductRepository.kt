package com.live.commerce.mall.repository

import com.live.commerce.mall.entity.Product
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional

interface ProductRepository : JpaRepository<Product, Long> {

    fun findByRoomId(roomId: Long): List<Product>
    fun findByRoomIdAndStatus(roomId: Long, status: Int): List<Product>

    fun findByNameContaining(name: String, pageable: Pageable): Page<Product>

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.stock = p.stock - :quantity WHERE p.id = :id AND p.stock >= :quantity")
    fun deductStock(@Param("id") id: Long, @Param("quantity") quantity: Int): Int

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.stock = p.stock + :quantity WHERE p.id = :id")
    fun restoreStock(@Param("id") id: Long, @Param("quantity") quantity: Int): Int
}
