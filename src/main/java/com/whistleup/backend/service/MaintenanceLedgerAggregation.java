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
     * Per-flat fixed component for notebook display: prefers persisted {@code fixedMaintenance};
     * otherwise average of max(0, amount - water - appliances) per row.
     */
    public static BigDecimal resolveFixedPerFlat(List<Maintenance> rows, int scale, RoundingMode mode) {
        if (CollectionUtils.isEmpty(rows)) {
            return BigDecimal.ZERO;
        }
        BigDecimal fixedSum = BigDecimal.ZERO;
        int fixedCount = 0;
        for (Maintenance m : rows) {
            if (m.getFixedMaintenance() != null && m.getFixedMaintenance().compareTo(BigDecimal.ZERO) > 0) {
                fixedSum = fixedSum.add(m.getFixedMaintenance());
                fixedCount++;
            }
        }
        if (fixedCount > 0) {
            return fixedSum.divide(BigDecimal.valueOf(fixedCount), scale, mode);
        }
        BigDecimal baseTotal = BigDecimal.ZERO;
        for (Maintenance m : rows) {
            BigDecimal a = safe(m.getAmount());
            BigDecimal w = safe(m.getWaterAmount());
            BigDecimal app = safe(m.getAppliancesAmount());
            BigDecimal base = a.subtract(w).subtract(app);
            if (base.compareTo(BigDecimal.ZERO) < 0) {
                base = BigDecimal.ZERO;
            }
            baseTotal = baseTotal.add(base);
        }
        int n = rows.size();
        return n <= 0 ? BigDecimal.ZERO : baseTotal.divide(BigDecimal.valueOf(n), scale, mode);
    }

    /**
     * Non-water, non-appliances portion of total billed (matches collection line "Fixed Maintenance").
     */
    public static double fixedMaintenanceCollectionTotal(List<Maintenance> rows) {
        double total = sumAmounts(rows).doubleValue();
        double water = sumWater(rows).doubleValue();
        double appliances = sumAppliances(rows).doubleValue();
        double remainder = total - water - appliances;
        return Math.max(0, remainder);
    }

    private static BigDecimal safe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
