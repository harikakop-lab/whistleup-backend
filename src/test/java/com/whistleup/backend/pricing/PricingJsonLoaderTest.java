package com.whistleup.backend.pricing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whistleup.backend.resource.HomeServiceCatalogLineResource;
import com.whistleup.backend.resource.HomeServicePricedOptionResource;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PricingJsonLoaderTest {

    @Test
    void buildServiceLines_groupsByServiceAndVariant_applianceAlias() throws Exception {
        String json =
                """
                [
                  {"Category":"Appliance Service","Subcategory":"Refrigerator Repairing","Service Name":"Gas refill","Sub-Subcategory":"","Option Name":"Refill","Offer Price":"1500"},
                  {"Category":"Appliance Service","Subcategory":"Refrigerator Repairing","Service Name":"Gas refill","Sub-Subcategory":"","Option Name":"Visit","Offer Price":"199"},
                  {"Category":"Appliance Service","Subcategory":"Refrigerator Repairing","Service Name":"Other repair","Sub-Subcategory":"Deep","Option Name":"Std","Offer Price":"99"}
                ]
                """;
        PricingJsonLoader loader = new PricingJsonLoader(new ObjectMapper());
        loader.loadFromStream(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        List<HomeServiceCatalogLineResource> lines = loader.buildServiceLines(
                "appliance-service",
                "Appliances",
                "refrigerator-repairing",
                "Refrigerator Repairing");

        assertThat(lines).hasSize(2);
        HomeServiceCatalogLineResource gasLine = lines.stream()
                .filter(l -> "Gas refill".equals(l.getServiceName()))
                .findFirst()
                .orElseThrow();
        assertThat(gasLine.getPricedOptions()).hasSize(2);
        assertThat(gasLine.getPricedOptions().stream().map(HomeServicePricedOptionResource::getPrice))
                .containsExactly(1500, 199);
        for (HomeServicePricedOptionResource po : gasLine.getPricedOptions()) {
            assertThat(po.getId()).startsWith("j-");
        }
    }

    @Test
    void buildServiceLines_unknownSubcategory_returnsEmpty() throws Exception {
        String json =
                """
                [{"Category":"Cleaning","Subcategory":"Bathroom Cleaning","Service Name":"X","Sub-Subcategory":"","Option Name":"Y","Offer Price":"1"}]
                """;
        PricingJsonLoader loader = new PricingJsonLoader(new ObjectMapper());
        loader.loadFromStream(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        List<HomeServiceCatalogLineResource> lines = loader.buildServiceLines(
                "cleaning", "Cleaning", "sofa-cleaning", "Sofa Cleaning");

        assertThat(lines).isEmpty();
    }
}
