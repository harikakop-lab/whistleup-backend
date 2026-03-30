package com.whistleup.backend.repository;

import com.whistleup.backend.entity.Advertisement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdsRepository extends JpaRepository<Advertisement, Long> {
}
