package com.whistleup.backend.repository;

import com.whistleup.backend.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository
        extends JpaRepository<NotificationEntity, Long> {

    List<NotificationEntity> findByPhoneOrderByCreatedAtDesc(String phone);

    @Query("""
        SELECT n FROM NotificationEntity n
        WHERE n.type = 'MAINTENANCE'
          AND n.read = false
          AND (n.lastRemindedAt IS NULL OR n.lastRemindedAt < :threshold)
    """)
    List<NotificationEntity> findPendingMaintenance(
            @Param("threshold") LocalDateTime threshold
    );
}
