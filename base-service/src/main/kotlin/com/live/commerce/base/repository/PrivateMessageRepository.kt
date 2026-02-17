package com.live.commerce.base.repository

import com.live.commerce.base.entity.PrivateMessage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PrivateMessageRepository : JpaRepository<PrivateMessage, Long> {
    @Query(
        """
        SELECT m FROM PrivateMessage m 
        WHERE (m.senderId = :userA AND m.receiverId = :userB) 
           OR (m.senderId = :userB AND m.receiverId = :userA)
        ORDER BY m.createdAt ASC
        """
    )
    fun findConversation(@Param("userA") userA: Long, @Param("userB") userB: Long): List<PrivateMessage>

    @Query(
        """
        SELECT m FROM PrivateMessage m
        WHERE m.id IN (
            SELECT MAX(i.id) FROM PrivateMessage i
            WHERE i.senderId = :userId OR i.receiverId = :userId
            GROUP BY CASE WHEN i.senderId = :userId THEN i.receiverId ELSE i.senderId END
        )
        ORDER BY m.createdAt DESC
        """
    )
    fun findLatestConversations(@Param("userId") userId: Long): List<PrivateMessage>
}
