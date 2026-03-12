package com.whistleup.backend.repository;

import com.whistleup.backend.entity.Ledger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LedgerRepository extends JpaRepository<Ledger, Long> {

    Optional<Ledger> findByYearAndMonth(int year, String month);

    @Query("""
        select l from Ledger l
        left join fetch l.items
        where l.id = :ledgerId
    """)
    Optional<Ledger> findByIdWithItems(Long ledgerId);
}
