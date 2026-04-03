package com.whistleup.backend.service;

import com.whistleup.backend.entity.HomeServiceCategory;
import com.whistleup.backend.entity.HomeServiceSubcategory;
import com.whistleup.backend.pricing.PricingJsonLoader;
import com.whistleup.backend.repository.HomeServiceCategoryRepository;
import com.whistleup.backend.resource.HomeServiceCatalogLineResource;
import com.whistleup.backend.resource.HomeServiceCategoryResource;
import com.whistleup.backend.resource.HomeServiceOptionResource;
import com.whistleup.backend.resource.HomeServicePricedOptionResource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class HomeServicesCatalogService {

    private final HomeServiceCategoryRepository homeServiceCategoryRepository;
    private final PricingJsonLoader pricingJsonLoader;

    public List<HomeServiceCategoryResource> getCatalog() {
        List<HomeServiceCategory> categories =
                homeServiceCategoryRepository.findAllByActiveTrueOrderBySortOrderAsc();
        return categories.stream().map(this::toCategoryResource).toList();
    }

    private HomeServiceCategoryResource toCategoryResource(HomeServiceCategory c) {
        String subtitle = c.getSubtitle();
        if (subtitle == null || subtitle.isBlank()) {
            subtitle = "Professional " + c.getCategoryLabel() + " services at your doorstep.";
        }
        String icon = c.getCategoryIcon();
        if (icon == null || icon.isBlank()) {
            icon = "tools";
        }
        List<HomeServiceOptionResource> options = c.getSubcategories().stream()
                .filter(s -> Boolean.TRUE.equals(s.getActive()))
                .sorted(Comparator.comparing(HomeServiceSubcategory::getSortOrder))
                .map(s -> toOptionResource(c, s))
                .toList();
        return HomeServiceCategoryResource.builder()
                .key(c.getCategoryKey())
                .label(c.getCategoryLabel())
                .subtitle(subtitle)
                .icon(icon)
                .options(options)
                .build();
    }

    private HomeServiceOptionResource toOptionResource(HomeServiceCategory category, HomeServiceSubcategory s) {
        List<HomeServiceCatalogLineResource> lines = pricingJsonLoader.buildServiceLines(
                category.getCategoryKey(),
                category.getCategoryLabel(),
                s.getSubcategoryKey(),
                s.getSubcategoryLabel());
        if (lines.isEmpty()) {
            lines = List.of(defaultZeroPriceLine(s.getSubcategoryKey()));
        }
        int minPrice = lines.stream()
                .flatMap(l -> l.getPricedOptions().stream())
                .mapToInt(HomeServicePricedOptionResource::getPrice)
                .filter(Objects::nonNull)
                .min()
                .orElse(0);
        String imageUrl = s.getImage();
        if (imageUrl != null && imageUrl.isBlank()) {
            imageUrl = null;
        }
        return HomeServiceOptionResource.builder()
                .id(s.getSubcategoryKey())
                .title(s.getSubcategoryLabel())
                .description(s.getDescription())
                .price(minPrice)
                .image(imageUrl)
                .popular(Boolean.TRUE.equals(s.getPopular()))
                .serviceLines(lines)
                .build();
    }

    private static HomeServiceCatalogLineResource defaultZeroPriceLine(String subcategoryKey) {
        return HomeServiceCatalogLineResource.builder()
                .id(subcategoryKey + "/standard")
                .serviceName("Standard")
                .variantLabel(null)
                .description(null)
                .pricedOptions(List.of(
                        HomeServicePricedOptionResource.builder()
                                .id("j-0-" + subcategoryKey)
                                .label("Standard")
                                .price(0)
                                .build()))
                .build();
    }
}
