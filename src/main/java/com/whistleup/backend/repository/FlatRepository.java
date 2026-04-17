package com.whistleup.backend.repository;

import com.whistleup.backend.entity.FlatDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlatRepository extends JpaRepository<FlatDetails, Long> {
    Optional<FlatDetails> findFlatByFlatNumber(String flatNumber);

    Optional<FlatDetails> findByResident_Phone(String phone);

    Optional<FlatDetails> findByBuilding_BuildingIdAndFlatNumber(Long buildingId, String flatNumber);

    @Query(value = "SELECT * FROM flat_details WHERE BUILDING_ID = :buildingId", nativeQuery = true)
    Optional<List<FlatDetails>> findFlatsByBuilding(@Param("buildingId") Long buildingId);
}
