package com.whistleup.backend.repository;

import com.whistleup.backend.entity.VisitorEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface VisitorEntryRepository extends JpaRepository<VisitorEntry, Long> {

    List<VisitorEntry> findByBuilding_BuildingIdAndVisitAtGreaterThanEqualAndVisitAtLessThanOrderByVisitAtDesc(
            Long buildingId,
            Instant startInclusive,
            Instant endExclusive);
}
