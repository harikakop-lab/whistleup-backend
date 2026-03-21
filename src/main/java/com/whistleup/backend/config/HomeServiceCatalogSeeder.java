package com.whistleup.backend.config;

import com.whistleup.backend.entity.HomeServiceCatalogItem;
import com.whistleup.backend.repository.HomeServiceCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Configuration
@RequiredArgsConstructor
public class HomeServiceCatalogSeeder {

        private final HomeServiceCatalogRepository homeServiceCatalogRepository;

        @Bean
        public CommandLineRunner seedHomeServiceCatalog() {
                return args -> {
                        if (homeServiceCatalogRepository.count() > 0) {
                                return;
                        }
                        List<HomeServiceCatalogItem> items = new ArrayList<>();

                        addCategory(items, "cleaning", "Cleaning", "broom",
                                        List.of(
                                                        "Bathroom Cleaning", "Sofa Cleaning", "Kitchen Cleaning",
                                                        "Vacant Home Deep Cleaning", "Occupied Home Deep Cleaning",
                                                        "After Interior Deep Cleaning", "Office Cleaning",
                                                        "Mattress Cleaning",
                                                        "Mini Cleaning Services", "Floor Cleaning", "Terrace Cleaning",
                                                        "Tank and Sump Cleaning", "Bathroom & Kitchen Cleaning",
                                                        "Villa Cleaning", "Sofa & Mattress Cleaning",
                                                        "Fridge Cleaning Service", "Carpet Cleaning"));

                        addCategory(items, "painting", "Painting", "roller",
                                        List.of(
                                                        "Interior Texture", "Wood Polish", "Waterproofing", "Wallpaper",
                                                        "Grouting Services", "Rental Painting", "Exterior Painting",
                                                        "Exterior Texture", "Vacant Flat Painting", "Interior Painting",
                                                        "1 Day Painting"));

                        addCategory(items, "pest-control", "Pest Control", "bug-outline",
                                        List.of(
                                                        "Cockroach Control", "Termite Control",
                                                        "Commercial Pest Control",
                                                        "Bedbugs Control", "Mosquitoes Control", "Woodborer Control"));

                        addCategory(items, "floor-polishing", "Floor Polishing", "floor-plan",
                                        List.of(
                                                        "Mosaic Floor Polishing", "Indian Marble Floor Polishing",
                                                        "Italian Marble floor Polishing", "Granite Floor Polishing"));

                        addCategory(items, "appliance-service", "Appliances", "tools",
                                        List.of(
                                                        "Refrigerator Repairing", "Window AC Service",
                                                        "Washing Machine Repairing",
                                                        "Split AC Service", "Geyser Repairing"));

                        addCategory(items, "home-repair-services", "Home Repairs", "home-repair-service",
                                        List.of(
                                                        "Electrical work", "Plumbing Work", "Carpenter work",
                                                        "Bird Netting"));

                        addCategory(items, "packers-movers", "Packers & Movers", "truck-fast-outline",
                                        List.of("Within City", "Between City", "Within Society"));

                        addCategory(items, "facility-management", "Facility Management", "office-building-cog-outline",
                                        List.of("Housekeeping Service", "Hospital Facility Management"));

                        homeServiceCatalogRepository.saveAll(items);
                };
        }

        private void addCategory(
                        List<HomeServiceCatalogItem> items,
                        String categoryKey,
                        String categoryLabel,
                        String icon,
                        List<String> subcategories) {
                for (int i = 0; i < subcategories.size(); i++) {
                        String sub = subcategories.get(i);
                        items.add(
                                        HomeServiceCatalogItem.builder()
                                                        .categoryKey(categoryKey)
                                                        .categoryLabel(categoryLabel)
                                                        .categoryIcon(icon)
                                                        .subcategoryKey(slugify(sub))
                                                        .subcategoryLabel(sub)
                                                        .description("Professional " + sub + " service")
                                                        .price(0)
                                                        .image(defaultImageForCategory(categoryKey))
                                                        .popular(i == 0)
                                                        .sortOrder(i + 1)
                                                        .active(true)
                                                        .build());
                }
        }

        private String slugify(String text) {
                String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
                return normalized.toLowerCase(Locale.ENGLISH)
                                .replace("&", "and")
                                .replaceAll("[^a-z0-9]+", "-")
                                .replaceAll("(^-|-$)", "");
        }

        private String defaultImageForCategory(String categoryKey) {
                return switch (categoryKey) {
                        case "cleaning" ->
                                "https://images.unsplash.com/photo-1527515637462-cff94eecc1ac?auto=format&fit=crop&w=1200&q=80";
                        case "painting" ->
                                "https://images.unsplash.com/photo-1562259949-e8e7689d7828?auto=format&fit=crop&w=1200&q=80";
                        case "pest-control" ->
                                "https://images.unsplash.com/photo-1581578731548-c64695cc6952?auto=format&fit=crop&w=1200&q=80";
                        case "floor-polishing" ->
                                "https://images.unsplash.com/photo-1616486029423-aaa4789e8c9a?auto=format&fit=crop&w=1200&q=80";
                        case "appliance-service" ->
                                "https://images.unsplash.com/photo-1581578731548-52f8d69d89f1?auto=format&fit=crop&w=1200&q=80";
                        case "home-repair-services" ->
                                "https://images.unsplash.com/photo-1504307651254-35680f356dfd?auto=format&fit=crop&w=1200&q=80";
                        case "packers-movers" ->
                                "https://images.unsplash.com/photo-1611080626919-7cf5a9dbab5b?auto=format&fit=crop&w=1200&q=80";
                        default ->
                                "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80";
                };
        }
}
