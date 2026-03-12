package com.whistleup.backend.repository;

import com.whistleup.backend.entity.ComplaintImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintImageRepository extends JpaRepository<ComplaintImage, Long> {

}
