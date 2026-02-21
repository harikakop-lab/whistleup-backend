package com.whistleup.backend.repository;

import com.whistleup.backend.entity.DeviceTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceTokenRepository
        extends JpaRepository<DeviceTokenEntity, Long> {

    /* ---------------------------------
       Core queries
    ---------------------------------- */

    Optional<DeviceTokenEntity> findByExpoPushToken(String expoPushToken);

    @Query("""
        select d.expoPushToken
        from DeviceTokenEntity d
        where d.userId = :userId
          and d.active = true
    """)
    List<String> findActiveTokensByUserId(@Param("userId") Long userId);

    /* ---------------------------------
       MySQL UPSERT (used on login)
    ---------------------------------- */

    @Modifying
    @Query(
        value = """
        INSERT INTO device_tokens
            (user_id, expo_push_token, platform, active, last_seen)
        VALUES
            (:userId, :token, :platform, true, CURRENT_TIMESTAMP)
        ON DUPLICATE KEY UPDATE
            user_id = VALUES(user_id),
            platform = VALUES(platform),
            active = true,
            last_seen = CURRENT_TIMESTAMP
        """,
        nativeQuery = true
    )
    void saveOrUpdate(
        @Param("userId") Long userId,
        @Param("token") String token,
        @Param("platform") String platform
    );

    /* ---------------------------------
       Maintenance / cleanup
    ---------------------------------- */

    @Modifying
    @Query("""
        update DeviceTokenEntity d
        set d.active = false
        where d.expoPushToken = :token
    """)
    void deactivateByToken(@Param("token") String token);
}