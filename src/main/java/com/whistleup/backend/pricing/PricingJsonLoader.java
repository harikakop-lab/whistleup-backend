package com.whistleup.backend.pricing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whistleup.backend.resource.HomeServiceCatalogLineResource;
import com.whistleup.backend.resource.HomeServicePricedOptionResource;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.HashSet;
import java.util.Set;

/**
 * Loads {@code classpath:pricing.json} once and builds {@link HomeServiceCatalogLineResource} trees
 * keyed by DB category + subcategory labels (trimmed).
 */
@Slf4j
@Component
public class PricingJsonLoader {

    private static final String PRICING_RESOURCE = "pricing.json";

    /** DB category_key → JSON "Category" values to try (order matters). */
    private static final Map<String, List<String>> CATEGORY_KEY_ALIASES = Map.ofEntries(
            Map.entry("cleaning", List.of("Cleaning")),
            Map.entry("pest-control", List.of("Pest Control")),
            Map.entry("floor-polishing", List.of("Floor Polishing")),
            Map.entry("appliance-service", List.of("Appliance Service", "Appliances")),
            Map.entry("home-repair-services", List.of("Home Repair Services", "Home Repairs")),
            Map.entry("painting", List.of("Painting")),
            Map.entry("packers-movers", List.of("Packers & Movers", "Packers and Movers")),
            Map.entry("facility-management", List.of("Facility Management"))
    );

    private final ObjectMapper objectMapper;

    /**
     * Outer: trimmed JSON Category. Inner: trimmed JSON Subcategory → rows in file order.
     */
    private Map<String, Map<String, List<PricingJsonRow>>> index = Map.of();

    public PricingJsonLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void loadFromClasspath() {
        try (InputStream in = new ClassPathResource(PRICING_RESOURCE).getInputStream()) {
            loadFromStream(in);
        } catch (IOException e) {
            log.error("Failed to load {} — catalog prices will fall back to 0", PRICING_RESOURCE, e);
            index = Map.of();
        }
    }

    void loadFromStream(InputStream in) throws IOException {
        List<Map<String, String>> raw = objectMapper.readValue(in, new TypeReference<>() {});
        Map<String, Map<String, List<PricingJsonRow>>> built = new HashMap<>();
        int skippedEmptyCategory = 0;
        for (Map<String, String> m : raw) {
            String cat = trimToEmpty(m.get("Category"));
            if (cat.isEmpty()) {
                skippedEmptyCategory++;
                continue;
            }
            String sub = trimToEmpty(m.get("Subcategory"));
            String service = Objects.toString(m.get("Service Name"), "").trim();
            String variant = Objects.toString(m.get("Sub-Subcategory"), "").trim();
            String option = Objects.toString(m.get("Option Name"), "").trim();
            int price = parsePrice(m.get("Offer Price"));
            PricingJsonRow row = new PricingJsonRow(cat, sub, service, variant, option, price);
            built.computeIfAbsent(cat, k -> new HashMap<>())
                    .computeIfAbsent(sub, k -> new ArrayList<>())
                    .add(row);
        }
        if (skippedEmptyCategory > 0) {
            log.warn("Skipped {} pricing rows with blank Category", skippedEmptyCategory);
        }
        index = built;
        log.info("Loaded pricing.json: {} JSON categories", index.size());
    }

    /**
     * Lines + priced options from JSON for this DB subcategory, or empty if no match.
     */
    public List<HomeServiceCatalogLineResource> buildServiceLines(
            String categoryKey,
            String categoryLabel,
            String subcategoryKey,
            String subcategoryLabel) {
        List<PricingJsonRow> rows = findRows(categoryKey, categoryLabel, subcategoryLabel);
        if (rows.isEmpty()) {
            return List.of();
        }
        return groupIntoLines(subcategoryKey, categoryKey, rows);
    }

    private List<PricingJsonRow> findRows(
            String categoryKey, String categoryLabel, String subcategoryLabel) {
        String subNorm = trimToEmpty(subcategoryLabel);
        List<String> jsonCategories = resolveJsonCategoryNames(categoryKey, categoryLabel);
        for (String jc : jsonCategories) {
            Map<String, List<PricingJsonRow>> subs = index.get(jc);
            if (subs == null) {
                continue;
            }
            List<PricingJsonRow> direct = subs.get(subNorm);
            if (direct != null && !direct.isEmpty()) {
                return direct;
            }
            for (Map.Entry<String, List<PricingJsonRow>> e : subs.entrySet()) {
                if (e.getKey().trim().equals(subNorm)) {
                    return e.getValue();
                }
            }
        }
        return List.of();
    }

    private List<String> resolveJsonCategoryNames(String categoryKey, String categoryLabel) {
        List<String> fromKey = CATEGORY_KEY_ALIASES.get(categoryKey);
        if (fromKey != null && !fromKey.isEmpty()) {
            return fromKey;
        }
        String label = trimToEmpty(categoryLabel);
        if (!label.isEmpty()) {
            return List.of(label);
        }
        return List.of();
    }

    private List<HomeServiceCatalogLineResource> groupIntoLines(
            String subcategoryKey, String categoryKey, List<PricingJsonRow> rows) {
        record LineKey(String service, String variant) {}
        Map<LineKey, List<PricingJsonRow>> grouped = new LinkedHashMap<>();
        for (PricingJsonRow row : rows) {
            LineKey lk = new LineKey(row.serviceName(), row.variant());
            grouped.computeIfAbsent(lk, k -> new ArrayList<>()).add(row);
        }
        Set<String> usedSlugs = new HashSet<>();
        List<HomeServiceCatalogLineResource> out = new ArrayList<>();
        for (Map.Entry<LineKey, List<PricingJsonRow>> e : grouped.entrySet()) {
            LineKey lk = e.getKey();
            List<PricingJsonRow> lineRows = e.getValue();
            String lineSlug = uniqueSlug(
                    slugify(lk.service() + "-" + lk.variant()), usedSlugs);
            List<HomeServicePricedOptionResource> priced = new ArrayList<>();
            for (int i = 0; i < lineRows.size(); i++) {
                PricingJsonRow r = lineRows.get(i);
                String id = stableOptionId(categoryKey, subcategoryKey, r, i);
                priced.add(HomeServicePricedOptionResource.builder()
                        .id(id)
                        .label(r.optionName().isEmpty() ? "Option" : r.optionName())
                        .price(r.price())
                        .build());
            }
            String variantLabel = lk.variant().isEmpty() ? null : lk.variant();
            String serviceName = lk.service().isEmpty() ? "Service" : lk.service();
            out.add(HomeServiceCatalogLineResource.builder()
                    .id(subcategoryKey + "/" + lineSlug)
                    .serviceName(serviceName)
                    .variantLabel(variantLabel)
                    .description(null)
                    .pricedOptions(priced)
                    .build());
        }
        return out;
    }

    private static String uniqueSlug(String base, Set<String> used) {
        if (base.isBlank()) {
            base = "line";
        }
        String s = base;
        int n = 2;
        while (!used.add(s)) {
            s = base + "-" + n++;
        }
        return s;
    }

    private static String stableOptionId(
            String categoryKey, String subcategoryKey, PricingJsonRow row, int indexInLine) {
        String payload = String.join(
                "|",
                categoryKey,
                subcategoryKey,
                row.serviceName(),
                row.variant(),
                row.optionName(),
                Integer.toString(row.price()),
                Integer.toString(indexInLine));
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(payload.getBytes(StandardCharsets.UTF_8));
            return "j-" + HexFormat.of().formatHex(digest, 0, 12);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static int parsePrice(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        String s = raw.trim().replace(",", "");
        try {
            double d = Double.parseDouble(s);
            return (int) Math.round(d);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String trimToEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    private static String slugify(String text) {
        if (text == null || text.isBlank()) {
            return "line";
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.toLowerCase(Locale.ENGLISH)
                .replace("&", "and")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    public record PricingJsonRow(
            String jsonCategory,
            String jsonSubcategory,
            String serviceName,
            String variant,
            String optionName,
            int price) {}
}
