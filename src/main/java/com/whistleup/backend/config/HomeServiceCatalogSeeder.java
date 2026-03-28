package com.whistleup.backend.config;

import com.whistleup.backend.entity.HomeServiceCatalogLine;
import com.whistleup.backend.entity.HomeServiceCategory;
import com.whistleup.backend.entity.HomeServicePricedOption;
import com.whistleup.backend.entity.HomeServiceSubcategory;
import com.whistleup.backend.repository.HomeServiceCategoryRepository;
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

    private final HomeServiceCategoryRepository homeServiceCategoryRepository;

    private record PricedSeed(String key, String label, int price) {}

    @Bean
    public CommandLineRunner seedHomeServiceCatalog() {
        return args -> {
            if (homeServiceCategoryRepository.count() > 0) {
                return;
            }
            List<HomeServiceCategory> categories = new ArrayList<>();

            HomeServiceCategory cleaning = buildCategory("cleaning", "Cleaning", "broom", 1);
            List<String> cleaningSubs = List.of(
                    "Bathroom Cleaning", "Sofa Cleaning", "Kitchen Cleaning",
                    "Vacant Home Deep Cleaning", "Occupied Home Deep Cleaning",
                    "After Interior Deep Cleaning", "Office Cleaning",
                    "Mattress Cleaning",
                    "Mini Cleaning Services", "Floor Cleaning", "Terrace Cleaning",
                    "Tank and Sump Cleaning", "Bathroom & Kitchen Cleaning",
                    "Villa Cleaning", "Sofa & Mattress Cleaning",
                    "Fridge Cleaning Service", "Carpet Cleaning");
            for (int i = 0; i < cleaningSubs.size(); i++) {
                String sub = cleaningSubs.get(i);
                if ("Bathroom Cleaning".equals(sub)) {
                    addBathroomCleaningSubcategory(cleaning, i + 1);
                } else {
                    addSimpleSubcategory(cleaning, sub, i + 1, i == 0);
                }
            }
            categories.add(cleaning);

            categories.add(buildCategoryWithSimpleSubs("painting", "Painting", "roller", 2, List.of(
                    "Interior Texture", "Wood Polish", "Waterproofing", "Wallpaper",
                    "Grouting Services", "Rental Painting", "Exterior Painting",
                    "Exterior Texture", "Vacant Flat Painting", "Interior Painting",
                    "1 Day Painting")));

            categories.add(buildCategoryWithSimpleSubs("pest-control", "Pest Control", "bug-outline", 3, List.of(
                    "Cockroach Control", "Termite Control",
                    "Commercial Pest Control",
                    "Bedbugs Control", "Mosquitoes Control", "Woodborer Control")));

            categories.add(buildCategoryWithSimpleSubs("floor-polishing", "Floor Polishing", "floor-plan", 4, List.of(
                    "Mosaic Floor Polishing", "Indian Marble Floor Polishing",
                    "Italian Marble floor Polishing", "Granite Floor Polishing")));

            categories.add(buildCategoryWithSimpleSubs("appliance-service", "Appliances", "tools", 5, List.of(
                    "Refrigerator Repairing", "Window AC Service",
                    "Washing Machine Repairing",
                    "Split AC Service", "Geyser Repairing")));

            categories.add(buildCategoryWithSimpleSubs("home-repair-services", "Home Repairs", "home-repair-service", 6, List.of(
                    "Electrical work", "Plumbing Work", "Carpenter work",
                    "Bird Netting")));

            categories.add(buildCategoryWithSimpleSubs("packers-movers", "Packers & Movers", "truck-fast-outline", 7, List.of(
                    "Within City", "Between City", "Within Society")));

            categories.add(buildCategoryWithSimpleSubs("facility-management", "Facility Management", "office-building-cog-outline", 8, List.of(
                    "Housekeeping Service", "Hospital Facility Management")));

            homeServiceCategoryRepository.saveAll(categories);
        };
    }

    private HomeServiceCategory buildCategoryWithSimpleSubs(
            String categoryKey, String categoryLabel, String icon, int sortOrder, List<String> subLabels) {
        HomeServiceCategory cat = buildCategory(categoryKey, categoryLabel, icon, sortOrder);
        for (int i = 0; i < subLabels.size(); i++) {
            addSimpleSubcategory(cat, subLabels.get(i), i + 1, i == 0);
        }
        return cat;
    }

    private HomeServiceCategory buildCategory(String key, String label, String icon, int sortOrder) {
        return HomeServiceCategory.builder()
                .categoryKey(key)
                .categoryLabel(label)
                .categoryIcon(icon)
                .sortOrder(sortOrder)
                .active(true)
                .build();
    }

    private void addSimpleSubcategory(HomeServiceCategory cat, String subLabel, int order, boolean popular) {
        HomeServiceSubcategory s = HomeServiceSubcategory.builder()
                .subcategoryKey(slugify(subLabel))
                .subcategoryLabel(subLabel)
                .description("Professional " + subLabel + " service")
                .image(defaultImageForCategory(cat.getCategoryKey()))
                .popular(popular)
                .sortOrder(order)
                .active(true)
                .build();
        s.setCategory(cat);
        cat.getSubcategories().add(s);

        HomeServiceCatalogLine line = HomeServiceCatalogLine.builder()
                .lineKey("standard")
                .serviceName(subLabel)
                .variantLabel(null)
                .sortOrder(1)
                .active(true)
                .build();
        line.setSubcategory(s);
        s.getCatalogLines().add(line);

        HomeServicePricedOption po = HomeServicePricedOption.builder()
                .optionKey("standard")
                .optionLabel("Standard")
                .price(0)
                .sortOrder(1)
                .active(true)
                .build();
        po.setCatalogLine(line);
        line.getPricedOptions().add(po);
    }

    private void addBathroomCleaningSubcategory(HomeServiceCategory cat, int order) {
        String subLabel = "Bathroom Cleaning";
        HomeServiceSubcategory s = HomeServiceSubcategory.builder()
                .subcategoryKey(slugify(subLabel))
                .subcategoryLabel(subLabel)
                .description("Select service type and number of bathrooms. Sample prices aligned to VHS-style SKUs; replace via admin/import.")
                .image(defaultImageForCategory("cleaning"))
                .popular(true)
                .sortOrder(order)
                .active(true)
                .build();
        s.setCategory(cat);
        cat.getSubcategories().add(s);

        addBathroomLine(s, "manual-deep-onetime", "Bathroom Manual Cleaning", "Deep cleaning — One time", List.of(
                new PricedSeed("1-bathroom", "1 Bathroom", 449),
                new PricedSeed("2-bathrooms", "2 Bathrooms", 599),
                new PricedSeed("3-bathrooms", "3 Bathrooms", 749),
                new PricedSeed("4-bathrooms", "4 Bathrooms", 899),
                new PricedSeed("5-bathrooms", "5 Bathrooms", 999)), 1);

        addBathroomLine(s, "manual-amc", "Bathroom Manual Cleaning", "AMC — 12 services", List.of(
                new PricedSeed("1-bathroom-amc", "1 Bathroom", 2499),
                new PricedSeed("2-bathrooms-amc", "2 Bathrooms", 3299)), 2);
    }

    private void addBathroomLine(
            HomeServiceSubcategory s,
            String lineKey,
            String serviceName,
            String variant,
            List<PricedSeed> opts,
            int lineOrder) {
        HomeServiceCatalogLine line = HomeServiceCatalogLine.builder()
                .lineKey(lineKey)
                .serviceName(serviceName)
                .variantLabel(variant)
                .sortOrder(lineOrder)
                .active(true)
                .build();
        line.setSubcategory(s);
        s.getCatalogLines().add(line);
        int i = 1;
        for (PricedSeed o : opts) {
            HomeServicePricedOption po = HomeServicePricedOption.builder()
                    .optionKey(o.key())
                    .optionLabel(o.label())
                    .price(o.price())
                    .sortOrder(i++)
                    .active(true)
                    .build();
            po.setCatalogLine(line);
            line.getPricedOptions().add(po);
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
