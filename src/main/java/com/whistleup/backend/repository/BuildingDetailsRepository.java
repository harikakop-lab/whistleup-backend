package com.whistleup.backend.repository;

import com.whistleup.backend.entity.BuildingDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BuildingDetailsRepository extends JpaRepository<BuildingDetails, Long> {
    Optional<BuildingDetails> findByProfileId(String username);

    @Query("SELECT b FROM BuildingDetails b WHERE b.adminPhone IS NOT NULL AND TRIM(b.adminPhone) = :phone")
    List<BuildingDetails> findByTrimmedAdminPhone(@Param("phone") String phone);
}
