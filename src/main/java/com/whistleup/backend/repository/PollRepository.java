package com.whistleup.backend.repository;

import com.whistleup.backend.entity.PollEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PollRepository extends JpaRepository<PollEntity, Long> {
    Optional<List<PollEntity>> findAllByBuildingId(String buildingId);
}
