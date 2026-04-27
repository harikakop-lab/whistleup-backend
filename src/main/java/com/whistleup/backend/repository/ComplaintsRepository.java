package com.whistleup.backend.repository;

import com.whistleup.backend.entity.Complaints;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintsRepository extends JpaRepository<Complaints, Long>, JpaSpecificationExecutor<Complaints> {
    List<Complaints> findByProfileIdAndBuildingIdOrderByComplaintIdDesc(String profileId, String buildingId);

    List<Complaints> findByAssigneeProfileAndBuildingIdOrderByComplaintIdDesc(String profileId, String buildingId);

    List<Complaints> findByProfileIdOrderByComplaintIdDesc(String profileId);

    List<Complaints> findByAssigneeProfileOrderByComplaintIdDesc(String profileId);

    List<Complaints> findByBuildingIdOrderByComplaintIdDesc(String buildingId);

    void deleteByProfileId(String profileId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Complaints c SET c.assigneeProfile = null WHERE c.assigneeProfile = :profileId")
    int clearAssigneeForProfile(@Param("profileId") String profileId);
}
