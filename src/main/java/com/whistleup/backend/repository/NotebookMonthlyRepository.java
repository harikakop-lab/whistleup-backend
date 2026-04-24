package com.whistleup.backend.repository;

import com.whistleup.backend.entity.NotebookMonthly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotebookMonthlyRepository extends JpaRepository<NotebookMonthly, Long> {

    Optional<NotebookMonthly> findByBuildingIdAndYearAndMonth(String buildingId, Integer year, String month);
}
