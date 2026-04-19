package com.whistleup.backend.repository;

import com.whistleup.backend.entity.PackersMoversInquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PackersMoversInquiryRepository extends JpaRepository<PackersMoversInquiry, Long> {
}
