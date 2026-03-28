package com.whistleup.backend.service;

import com.whistleup.backend.entity.HomeServiceCategory;
import com.whistleup.backend.entity.HomeServiceSubcategory;
import com.whistleup.backend.pricing.PricingJsonLoader;
import com.whistleup.backend.repository.HomeServiceCategoryRepository;
import com.whistleup.backend.resource.HomeServiceCatalogLineResource;
import com.whistleup.backend.resource.HomeServiceCategoryResource;
import com.whistleup.backend.resource.HomeServiceOptionResource;
import com.whistleup.backend.resource.HomeServicePricedOptionResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeServicesCatalogServiceTest {

    @Mock
    private HomeServiceCategoryRepository homeServiceCategoryRepository;

    @Mock
    private PricingJsonLoader pricingJsonLoader;

    @InjectMocks
    private HomeServicesCatalogService homeServicesCatalogService;

    @Test
    void getCatalog_usesJsonLines_imageNull_minPriceFromJson() {
        HomeServiceCategory category = HomeServiceCategory.builder()
                .categoryKey("pest-control")
                .categoryLabel("Pest Control")
                .categoryIcon("bug-outline")
                .sortOrder(1)
                .active(true)
                .build();

        HomeServiceSubcategory sub = HomeServiceSubcategory.builder()
                .subcategoryKey("cockroach-control")
                .subcategoryLabel("Cockroach Control")
                .description("desc")
                .image("https://db-should-be-ignored.jpg")
                .popular(true)
                .sortOrder(1)
                .active(true)
                .build();
        sub.setCategory(category);
        category.getSubcategories().add(sub);

        List<HomeServiceCatalogLineResource> jsonLines = List.of(
                HomeServiceCatalogLineResource.builder()
                        .id("cockroach-control/line-a")
                        .serviceName("Cockroach Pest Control")
                        .variantLabel(null)
                        .pricedOptions(List.of(
                                HomeServicePricedOptionResource.builder()
                                        .id("j-aaaaaaaaaaaa")
                                        .label("Kitchen")
                                        .price(599)
                                        .build(),
                                HomeServicePricedOptionResource.builder()
                                        .id("j-bbbbbbbbbbbb")
                                        .label("1 BHK")
                                        .price(999)
                                        .build()))
                        .build());

        when(homeServiceCategoryRepository.findAllByActiveTrueOrderBySortOrderAsc()).thenReturn(List.of(category));
        when(pricingJsonLoader.buildServiceLines(
                        "pest-control", "Pest Control", "cockroach-control", "Cockroach Control"))
                .thenReturn(jsonLines);

        List<HomeServiceCategoryResource> catalog = homeServicesCatalogService.getCatalog();

        assertThat(catalog).hasSize(1);
        HomeServiceOptionResource opt = catalog.get(0).getOptions().get(0);
        assertThat(opt.getId()).isEqualTo("cockroach-control");
        assertThat(opt.getPrice()).isEqualTo(599);
        assertThat(opt.getImage()).isNull();
        assertThat(opt.getServiceLines()).hasSize(1);
        assertThat(opt.getServiceLines().get(0).getPricedOptions()).hasSize(2);
        assertThat(opt.getServiceLines().get(0).getPricedOptions().get(0).getId()).startsWith("j-");
    }

    @Test
    void getCatalog_emptyJson_fallsBackToStandardZero() {
        HomeServiceCategory category = HomeServiceCategory.builder()
                .categoryKey("facility-management")
                .categoryLabel("Facility Management")
                .categoryIcon("office-building-cog-outline")
                .sortOrder(1)
                .active(true)
                .build();

        HomeServiceSubcategory sub = HomeServiceSubcategory.builder()
                .subcategoryKey("housekeeping-service")
                .subcategoryLabel("Housekeeping Service")
                .description("desc")
                .image("https://ignore.jpg")
                .popular(true)
                .sortOrder(1)
                .active(true)
                .build();
        sub.setCategory(category);
        category.getSubcategories().add(sub);

        when(homeServiceCategoryRepository.findAllByActiveTrueOrderBySortOrderAsc()).thenReturn(List.of(category));
        when(pricingJsonLoader.buildServiceLines(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(List.of());

        HomeServiceOptionResource opt =
                homeServicesCatalogService.getCatalog().get(0).getOptions().get(0);

        assertThat(opt.getPrice()).isZero();
        assertThat(opt.getImage()).isNull();
        assertThat(opt.getServiceLines()).hasSize(1);
        assertThat(opt.getServiceLines().get(0).getId()).isEqualTo("housekeeping-service/standard");
        assertThat(opt.getServiceLines().get(0).getPricedOptions().get(0).getPrice()).isZero();
        assertThat(opt.getServiceLines().get(0).getPricedOptions().get(0).getId()).isEqualTo("j-0-housekeeping-service");
    }
}
