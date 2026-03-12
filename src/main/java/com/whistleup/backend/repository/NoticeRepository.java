package com.whistleup.backend.repository;

import com.whistleup.backend.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NoticeRepository extends JpaRepository<Notice, UUID> {

    List<Notice> findByBuildingIdOrderByCreatedAtDesc(String buildingId);
}
