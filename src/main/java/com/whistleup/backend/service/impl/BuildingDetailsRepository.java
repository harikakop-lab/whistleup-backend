package com.whistleup.backend.service.impl;

import com.whistleup.backend.entity.BuildingDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BuildingDetailsRepository extends JpaRepository<BuildingDetails, String> {
}
