package com.whistleup.backend.repository;

import com.whistleup.backend.entity.Complaints;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ComplaintsRepository extends JpaRepository<Complaints, Long> {
    Optional<Complaints> findByProfileId(String profileId);
}
