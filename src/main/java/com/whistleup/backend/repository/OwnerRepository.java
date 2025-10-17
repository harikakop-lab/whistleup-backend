package com.whistleup.backend.repository;

import com.whistleup.backend.entity.OwnerDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OwnerRepository extends JpaRepository<OwnerDetails, Long> {
}
