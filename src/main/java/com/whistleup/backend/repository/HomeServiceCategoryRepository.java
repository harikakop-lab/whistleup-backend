package com.whistleup.backend.repository;

import com.whistleup.backend.entity.HomeServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HomeServiceCategoryRepository extends JpaRepository<HomeServiceCategory, Long> {
    List<HomeServiceCategory> findAllByActiveTrueOrderBySortOrderAsc();
}
