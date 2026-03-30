package com.whistleup.backend.repository;

import com.whistleup.backend.entity.Ledger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LedgerRepository extends JpaRepository<Ledger, Long> {

    Optional<Ledger> findByYearAndMonth(int year, String month);
    Optional<Ledger> findTopByYearAndMonthOrderByIdDesc(int year, String month);

    Optional<Ledger> findByYearAndMonthAndBuildingId(int year, String month, String buildingId);

    @Query("""
        select l from Ledger l
        left join fetch l.items
        where l.id = :ledgerId
    """)
    Optional<Ledger> findByIdWithItems(Long ledgerId);

    @Query("""
        select l from Ledger l
        left join fetch l.items
        where l.year = :year and l.month = :month and l.buildingId = :buildingId
    """)
    Optional<Ledger> findByYearAndMonthAndBuildingIdWithItems(int year, String month, String buildingId);
}
