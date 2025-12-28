package com.whistleup.backend.repository;

import com.whistleup.backend.controllers.ResidentsResponse;
import com.whistleup.backend.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, String> {
    Optional<Profile> findByEmail(String email);

    Optional<Profile> findByPhone(String phone);

    @Query("SELECT p FROM Profile p WHERE p.email = :loginId OR p.phone = :loginId")
    Optional<Profile> findByEmailOrPhone(@Param("loginId") String loginId);

    @Query("""
       select new com.whistleup.backend.controllers.ResidentsResponse(
       f.resident.phone, f.resident.name, f.floor)
       from FlatDetails f
       where f.building.buildingId = :buildingId
         and f.floor = :floorNo
       """)
    List<ResidentsResponse> getListOfResidents(Long buildingId, Long floorNo);

    @Query("""
       select new com.whistleup.backend.controllers.ResidentsResponse(
       f.resident.phone, f.resident.name, f.floor)
       from FlatDetails f
       where f.building.buildingId = :buildingId
       """)
    List<ResidentsResponse> getListOfResidentsByBuilding(Long buildingId);
}
