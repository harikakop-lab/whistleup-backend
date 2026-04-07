package com.whistleup.backend.repository;

import com.whistleup.backend.entity.QrBuildingMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QrBuildingMappingRepository extends JpaRepository<QrBuildingMapping, Long> {
    Optional<QrBuildingMapping> findByToken(String token);
}
