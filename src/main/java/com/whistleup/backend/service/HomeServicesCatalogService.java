package com.whistleup.backend.service;

import com.whistleup.backend.resource.HomeServiceCategoryResource;
import com.whistleup.backend.resource.HomeServiceOptionResource;
import com.whistleup.backend.entity.HomeServiceCatalogItem;
import com.whistleup.backend.repository.HomeServiceCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HomeServicesCatalogService {

    private final HomeServiceCatalogRepository homeServiceCatalogRepository;

    public List<HomeServiceCategoryResource> getCatalog() {
        List<HomeServiceCatalogItem> rows =
                homeServiceCatalogRepository.findAllByActiveTrueOrderByCategoryLabelAscSortOrderAscSubcategoryLabelAsc();
        Map<String, HomeServiceCategoryResource> grouped = new LinkedHashMap<>();
        for (HomeServiceCatalogItem row : rows) {
            HomeServiceCategoryResource category = grouped.computeIfAbsent(
                    row.getCategoryKey(),
                    key -> HomeServiceCategoryResource.builder()
                            .key(row.getCategoryKey())
                            .label(row.getCategoryLabel())
                            .subtitle("Professional " + row.getCategoryLabel() + " services at your doorstep.")
                            .icon(row.getCategoryIcon() == null || row.getCategoryIcon().isBlank() ? "tools" : row.getCategoryIcon())
                            .options(new java.util.ArrayList<>())
                            .build()
            );
            category.getOptions().add(
                    HomeServiceOptionResource.builder()
                            .id(row.getSubcategoryKey())
                            .title(row.getSubcategoryLabel())
                            .description(row.getDescription())
                            .price(row.getPrice() == null ? 0 : row.getPrice())
                            .image(row.getImage())
                            .popular(Boolean.TRUE.equals(row.getPopular()))
                            .build()
            );
        }
        return grouped.values().stream().toList();
    }
}
