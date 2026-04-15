package com.whistleup.backend.repository;

import com.whistleup.backend.controllers.ResidentsResponse;
import com.whistleup.backend.constants.Roles;
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
       f.resident.phone, f.resident.name, f.resident.flatNo)
       from FlatDetails f
       where f.building.buildingId = :buildingId
         and f.resident.flatNo = :flatNo
       """)
    List<ResidentsResponse> getListOfResidents(Long buildingId, Long flatNo);

    @Query("""
       select new com.whistleup.backend.controllers.ResidentsResponse(
       f.resident.phone, f.resident.name, f.resident.flatNo)
       from FlatDetails f
       where f.building.buildingId = :buildingId
       """)
    List<ResidentsResponse> getListOfResidentsByBuilding(Long buildingId);

    List<Profile> findByBuildingId(String buildingId);

    List<Profile> findByBuildingIdAndRole(String buildingId, Roles role);

    @Query("""
       select p from Profile p
       where p.buildingId = :buildingId
         and p.flatNo in :flatNos
       order by p.name asc
       """)
    List<Profile> findByBuildingIdAndFlatNoIn(@Param("buildingId") String buildingId,
                                              @Param("flatNos") List<String> flatNos);

    @Query("""
       select new com.whistleup.backend.controllers.ResidentsResponse(
       p.phone, p.name, p.flatNo)
       from Profile p
       where p.buildingId = :buildingId
         and p.isAssigned = false
         and p.role in :roles
       """)
    List<ResidentsResponse> getPendingResidentsByBuilding(@Param("buildingId") String buildingId,
                                                          @Param("roles") List<Roles> roles);
}
