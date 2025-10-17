package com.whistleup.backend.repository;

import com.whistleup.backend.entity.BuildingDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BuildingDetailsRepository extends JpaRepository<BuildingDetails, Long> {
}
