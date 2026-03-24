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

    Optional<DeviceTokenEntity> findByExpoPushToken(String expoPushToken);

    List<DeviceTokenEntity> findByUserIdAndActiveTrue(Long userId);

    void deleteAllByUserId(Long userId);

    @Modifying
    @Query("""
        update DeviceTokenEntity d
        set d.active = false
        where d.expoPushToken = :token
        """)
    void deactivateByToken(@Param("token") String token);

    @Modifying
    @Query("""
        update DeviceTokenEntity d
        set d.active = false
        where d.fcmToken = :token
        """)
    void deactivateByFcmToken(@Param("token") String token);
}
