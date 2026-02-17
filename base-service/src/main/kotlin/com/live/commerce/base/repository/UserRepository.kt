package com.live.commerce.base.repository

import com.live.commerce.base.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserRepository : JpaRepository<User, Long> {
    fun findByUsername(username: String): User?

    @Query(
        "SELECT u FROM User u WHERE (:keyword IS NULL OR u.username LIKE %:keyword% OR u.nickname LIKE %:keyword%)"
    )
    fun search(@Param("keyword") keyword: String?, pageable: Pageable): Page<User>
}
