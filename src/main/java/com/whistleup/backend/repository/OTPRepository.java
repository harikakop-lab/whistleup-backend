package com.whistleup.backend.repository;

import com.whistleup.backend.entity.OTPEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OTPRepository extends JpaRepository<OTPEntity, Long> {
    Optional<List<OTPEntity>> findByPhoneNumberOrderByLastResendTimeDesc(String phoneNumber);
    Optional<OTPEntity> findByPhoneNumberAndIsVerifiedFalse(String phoneNumber);
}