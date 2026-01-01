package com.whistleup.backend.repository;

import com.whistleup.backend.entity.Maintenance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaintenanceRepository extends JpaRepository<Maintenance, Long> {


    List<Maintenance> findByBuildingIdOrderByMaintenanceYearDescMaintenanceMonthDesc(
            String buildingId
    );


    // By Profile Id to display it to the user
    List<Maintenance> findByProfileIdOrderByMaintenanceYearDescMaintenanceMonthDesc(
            String buildingId
    );

    Optional<Maintenance> findByBuildingIdAndMaintenanceYearAndMaintenanceMonth(
            String buildingId,
            Integer maintenanceYear,
            Integer maintenanceMonth
    );
}
