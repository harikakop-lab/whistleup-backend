package com.whistleup.backend.repository;

import com.whistleup.backend.entity.HomeServiceCatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HomeServiceCatalogRepository extends JpaRepository<HomeServiceCatalogItem, Long> {
    List<HomeServiceCatalogItem> findAllByActiveTrueOrderByCategoryLabelAscSortOrderAscSubcategoryLabelAsc();
}
