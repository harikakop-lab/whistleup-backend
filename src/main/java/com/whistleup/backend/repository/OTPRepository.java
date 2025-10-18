package com.whistleup.backend.repository;

import com.whistleup.backend.entity.OTPEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface OTPRepository extends JpaRepository<OTPEntity, Long> {
    Optional<OTPEntity> findByPhoneNumber(String phoneNumber);
    Optional<OTPEntity> findByPhoneNumberAndIsVerifiedFalse(String phoneNumber);
}