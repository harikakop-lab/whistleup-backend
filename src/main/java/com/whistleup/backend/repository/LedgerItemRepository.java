package com.whistleup.backend.repository;

import com.whistleup.backend.entity.LedgerItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LedgerItemRepository extends JpaRepository<LedgerItem, Long> {
}
