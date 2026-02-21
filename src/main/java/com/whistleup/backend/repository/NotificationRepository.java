package com.whistleup.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository
        extends JpaRepository<com.whistleup.backend.notifications.entity.NotificationEntity, Long> {

    /* ---------------------------------
       App-facing queries
    ---------------------------------- */

    List<com.whistleup.backend.notifications.entity.NotificationEntity>
        findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndIsReadFalse(Long userId);

    List<com.whistleup.backend.notifications.entity.NotificationEntity>
        findByUserIdAndIsReadFalse(Long userId);

    /* ---------------------------------
       Bulk updates
    ---------------------------------- */

    @Modifying
    @Query("""
        update NotificationEntity n
        set n.isRead = true
        where n.userId = :userId
    """)
    void markAllAsRead(@Param("userId") Long userId);
}