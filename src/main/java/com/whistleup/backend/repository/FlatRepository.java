package com.whistleup.backend.repository;

import com.whistleup.backend.entity.FlatDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FlatRepository extends JpaRepository<FlatDetails, Long> {
    Optional<FlatDetails> findFlatByFlatNumber(String flatNumber);
}
