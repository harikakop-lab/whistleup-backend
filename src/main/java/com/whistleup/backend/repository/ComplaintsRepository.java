package com.whistleup.backend.repository;

import com.whistleup.backend.entity.Complaints;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintsRepository extends JpaRepository<Complaints, Long> {
    List<Complaints> findByProfileIdAndBuildingIdOrderByComplaintIdDesc(String profileId, String buildingId);

    List<Complaints> findByAssigneeProfileAndBuildingIdOrderByComplaintIdDesc(String profileId, String buildingId);

    List<Complaints> findByProfileIdOrderByComplaintIdDesc(String profileId);

    List<Complaints> findByAssigneeProfileOrderByComplaintIdDesc(String profileId);

    List<Complaints> findByBuildingIdOrderByComplaintIdDesc(String buildingId);
}
