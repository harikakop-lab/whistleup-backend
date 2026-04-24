package com.whistleup.backend.service;

import com.whistleup.backend.entity.Maintenance;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
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

    /**
     * Per-row fixed (non-water, non-appliances) charge: prefers persisted {@code fixedMaintenance} when set;
     * otherwise max(0, amount - water - appliances). This stays correct when {@code amount} was under-written
     * on a partial update but {@code fixedMaintenance} was preserved.
     */
    public static BigDecimal rowFixedPortion(Maintenance m) {
        if (m == null) {
            return BigDecimal.ZERO;
        }
        if (m.getFixedMaintenance() != null && m.getFixedMaintenance().compareTo(BigDecimal.ZERO) > 0) {
            return m.getFixedMaintenance();
        }
        BigDecimal implied = safe(m.getAmount()).subtract(safe(m.getWaterAmount())).subtract(safe(m.getAppliancesAmount()));
        return implied.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : implied;
    }

    public static BigDecimal sumFixedPortions(List<Maintenance> rows) {
        if (CollectionUtils.isEmpty(rows)) {
            return BigDecimal.ZERO;
        }
        return rows.stream().map(MaintenanceLedgerAggregation::rowFixedPortion).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Total expected collected from maintenance rows for ledger/notebook headers: sum of fixed portions + water + appliances.
     * Prefer this over {@link #sumAmounts} when persisted {@code amount} may not match the sum of components.
     */
    public static BigDecimal canonicalTotalCollected(List<Maintenance> rows) {
        if (CollectionUtils.isEmpty(rows)) {
            return BigDecimal.ZERO;
        }
        return sumFixedPortions(rows).add(sumWater(rows)).add(sumAppliances(rows));
    }

    /**
     * Per-flat average fixed charge for notebook display (same numerator as total fixed portions).
     */
    public static BigDecimal resolveFixedPerFlat(List<Maintenance> rows, int scale, RoundingMode mode) {
        if (CollectionUtils.isEmpty(rows)) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = sumFixedPortions(rows);
        return sum.divide(BigDecimal.valueOf(rows.size()), scale, mode);
    }

    /**
     * Aggregate fixed line for collection breakdown (ledger "Fixed Maintenance" row).
     */
    public static double fixedMaintenanceCollectionTotal(List<Maintenance> rows) {
        return sumFixedPortions(rows).doubleValue();
    }

    private static BigDecimal safe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
