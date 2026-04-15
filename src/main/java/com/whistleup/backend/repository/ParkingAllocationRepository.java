package com.whistleup.backend.repository;

import com.whistleup.backend.entity.ParkingAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParkingAllocationRepository extends JpaRepository<ParkingAllocation, Long> {

    List<ParkingAllocation> findByBuilding_BuildingIdOrderByCreatedAtDesc(Long buildingId);
}
