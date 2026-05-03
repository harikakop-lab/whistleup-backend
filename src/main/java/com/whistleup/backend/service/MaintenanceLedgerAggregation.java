package com.whistleup.backend.service;

import com.whistleup.backend.entity.Maintenance;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Shared aggregation rules so notebook persistence and apartment ledger stay aligned with
 * per-flat maintenance rows for a building/month.
 */
public final class MaintenanceLedgerAggregation {

    private MaintenanceLedgerAggregation() {
    }

    public static BigDecimal sumAmounts(List<Maintenance> rows) {
        if (CollectionUtils.isEmpty(rows)) {
            return BigDecimal.ZERO;
        }
        return rows.stream()
                .map(Maintenance::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static BigDecimal sumWater(List<Maintenance> rows) {
        if (CollectionUtils.isEmpty(rows)) {
            return BigDecimal.ZERO;
        }
        return rows.stream()
                .map(Maintenance::getWaterAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static BigDecimal sumAppliances(List<Maintenance> rows) {
        if (CollectionUtils.isEmpty(rows)) {
            return BigDecimal.ZERO;
        }
        return rows.stream()
                .map(Maintenance::getAppliancesAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Building-level non-split asset amount. Stored on each row, but counted once using max. */
    public static BigDecimal buildingAssetAmount(List<Maintenance> rows) {
        if (CollectionUtils.isEmpty(rows)) {
            return BigDecimal.ZERO;
        }
        return rows.stream()
                .map(Maintenance::getAssetAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::max);
    }

    /** Sum of per-flat custom expense map values on one maintenance row. */
    public static BigDecimal rowCustomSum(Maintenance m) {
        if (m == null || m.getCustomExpenses() == null || m.getCustomExpenses().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return m.getCustomExpenses().values().stream()
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Merges per-row custom labels into building-level totals (each row value is already per-flat).
     */
    public static Map<String, BigDecimal> mergeCustomExpenseTotals(List<Maintenance> rows) {
        Map<String, BigDecimal> out = new LinkedHashMap<>();
        if (CollectionUtils.isEmpty(rows)) {
            return out;
        }
        for (Maintenance m : rows) {
            if (m == null || m.getCustomExpenses() == null) {
                continue;
            }
            for (Map.Entry<String, BigDecimal> e : m.getCustomExpenses().entrySet()) {
                String key = e.getKey() == null ? "" : e.getKey().trim();
                if (key.isEmpty()) {
                    continue;
                }
                BigDecimal v = safe(e.getValue());
                if (v.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                out.merge(key, v, BigDecimal::add);
            }
        }
        return out;
    }

    /**
     * Fixed maintenance excluding custom lines (custom appears as separate collection rows in the ledger).
     */
    public static BigDecimal rowStandardFixedPortion(Maintenance m) {
        BigDecimal full = rowFixedPortion(m);
        BigDecimal custom = rowCustomSum(m);
        BigDecimal net = full.subtract(custom);
        return net.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : net;
    }

    public static BigDecimal sumStandardFixedPortions(List<Maintenance> rows) {
        if (CollectionUtils.isEmpty(rows)) {
            return BigDecimal.ZERO;
        }
        return rows.stream()
                .map(MaintenanceLedgerAggregation::rowStandardFixedPortion)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Per-row fixed (non-water, non-appliances) charge: uses the greater of persisted {@code fixedMaintenance}
     * (when set) and {@code amount - water - appliances}, so a correct total {@code amount} that includes custom
     * is not replaced by a {@code fixedMaintenance} value that only reflects standard lines.
     */
    public static BigDecimal rowFixedPortion(Maintenance m) {
        if (m == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal implied = safe(m.getAmount()).subtract(safe(m.getWaterAmount())).subtract(safe(m.getAppliancesAmount()));
        if (implied.compareTo(BigDecimal.ZERO) < 0) {
            implied = BigDecimal.ZERO;
        }
        if (m.getFixedMaintenance() != null && m.getFixedMaintenance().compareTo(BigDecimal.ZERO) > 0) {
            return implied.max(m.getFixedMaintenance());
        }
        return implied;
    }

    public static BigDecimal sumFixedPortions(List<Maintenance> rows) {
        if (CollectionUtils.isEmpty(rows)) {
            return BigDecimal.ZERO;
        }
        return rows.stream().map(MaintenanceLedgerAggregation::rowFixedPortion).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Total expected collected from maintenance rows for ledger/notebook headers: standard fixed (excluding
     * per-row custom map totals) + building total of custom + water + appliances. Matches the sum of collection
     * line items (Fixed Maintenance + each custom label + Water + Appliances).
     */
    public static BigDecimal canonicalTotalCollected(List<Maintenance> rows) {
        if (CollectionUtils.isEmpty(rows)) {
            return BigDecimal.ZERO;
        }
        BigDecimal fromCollectionLines = sumStandardFixedPortions(rows)
                .add(sumCustomExpensesAcrossRows(rows))
                .add(sumWater(rows))
                .add(sumAppliances(rows));
        return fromCollectionLines.max(sumAmounts(rows)).add(buildingAssetAmount(rows));
    }

    /** Building total of per-flat custom map values (each map entry is already split per flat). */
    public static BigDecimal sumCustomExpensesAcrossRows(List<Maintenance> rows) {
        if (CollectionUtils.isEmpty(rows)) {
            return BigDecimal.ZERO;
        }
        return rows.stream().map(MaintenanceLedgerAggregation::rowCustomSum).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Denominator for “per flat slot” averages: persisted {@code billedUnitCount} on any row for the period,
     * or legacy fallback to payer row count.
     */
    public static int resolveBilledUnitCount(List<Maintenance> rows) {
        if (CollectionUtils.isEmpty(rows)) {
            return 1;
        }
        for (Maintenance m : rows) {
            if (m != null && m.getBilledUnitCount() != null && m.getBilledUnitCount() > 0) {
                return m.getBilledUnitCount();
            }
        }
        return Math.max(1, rows.size());
    }

    /**
     * Per-flat average land charge before water/appliances (uses {@link #rowFixedPortion}, which may reflect
     * full {@code amount} when it exceeds {@code fixedMaintenance}).
     */
    public static BigDecimal resolveFixedPerFlat(List<Maintenance> rows, int scale, RoundingMode mode) {
        if (CollectionUtils.isEmpty(rows)) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = sumFixedPortions(rows);
        int divisor = resolveBilledUnitCount(rows);
        return sum.divide(BigDecimal.valueOf(divisor), scale, mode);
    }

    /**
     * Aggregate fixed line for collection breakdown (ledger "Fixed Maintenance" row), excluding custom
     * so custom keys can be listed without double counting against {@link #canonicalTotalCollected}.
     */
    public static double fixedMaintenanceCollectionTotal(List<Maintenance> rows) {
        return sumStandardFixedPortions(rows).doubleValue();
    }

    private static BigDecimal safe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
