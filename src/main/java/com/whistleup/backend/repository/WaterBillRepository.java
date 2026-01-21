package com.whistleup.backend.repository;

import com.whistleup.backend.entity.WaterBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WaterBillRepository extends JpaRepository<WaterBill, Long> {
}
