package com.whistleup.backend.repository;

import com.whistleup.backend.entity.Maintenance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaintenanceRepository extends JpaRepository<Maintenance, Long> {


    List<Maintenance> findByProfileIdOrderByMaintenanceYearDescMaintenanceMonthDesc(
            String profileId
    );

    Optional<Maintenance> findByProfileIdAndMaintenanceYearAndMaintenanceMonth(
            String profileId,
            Integer maintenanceYear,
            Integer maintenanceMonth
    );
}
