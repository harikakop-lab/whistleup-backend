package com.whistleup.backend.repository;

import com.whistleup.backend.constants.MaintenanceStatus;
import com.whistleup.backend.entity.Maintenance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaintenanceRepository extends JpaRepository<Maintenance, Long> {


    List<Maintenance> findByBuildingIdOrderByMaintenanceYearDescMaintenanceMonthDesc(
            String buildingId
    );


    // By Profile Id to display it to the user
    List<Maintenance> findByProfileIdOrderByMaintenanceYearDescMaintenanceMonthDesc(
            String buildingId
    );

    Optional<Maintenance> findByProfileIdAndBuildingIdAndMaintenanceYearAndMaintenanceMonth(
            String profileId,
            String buildingId,
            Integer maintenanceYear,
            Integer maintenanceMonth
    );

    List<Maintenance> findByBuildingIdAndMaintenanceYearAndMaintenanceMonth(
            String buildingId,
            Integer maintenanceYear,
            Integer maintenanceMonth
    );

    boolean existsByProfileIdAndStatus(String profileId, MaintenanceStatus status);

    void deleteByProfileId(String profileId);

    @Query(value = "select * from maintenance where status = 'PENDING' and maintenance_year = :year", nativeQuery = true)
    List<Maintenance> findPendingMaintenanceByPreviousMonthAndYear(int year);
}
