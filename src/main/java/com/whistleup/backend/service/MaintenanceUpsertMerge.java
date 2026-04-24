package com.whistleup.backend.service;

import com.whistleup.backend.resource.MaintenanceCreateResource;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Merge rules when re-saving maintenance for the same profile/month so partial payloads
 * (e.g. water-only) do not wipe an existing base charge.
 */
public final class MaintenanceUpsertMerge {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private MaintenanceUpsertMerge() {
    }

    public static boolean hasSharedExpensePayload(MaintenanceCreateResource req) {
        if (req == null) {
            return false;
        }
        if (req.getWatchmanSalary() != null) {
            return true;
        }
        if (req.getGarbageCollection() != null) {
            return true;
        }
        if (req.getLiftMaintenance() != null) {
            return true;
        }
        if (req.getElectricityCommon() != null) {
            return true;
        }
        if (req.getMotorPump() != null) {
            return true;
        }
        if (req.getMiscellaneous() != null) {
            return true;
        }
        Map<String, BigDecimal> custom = req.getCustomExpenses();
        return custom != null && !custom.isEmpty();
    }

    public static boolean hasWaterPayload(MaintenanceCreateResource req) {
        if (req == null) {
            return false;
        }
        String mode = req.getWaterMode() == null ? "" : req.getWaterMode().trim().toUpperCase(Locale.ROOT);
        if (!mode.isEmpty()) {
            return true;
        }
        if (positive(req.getFixedWaterBill()) || positive(req.getMasterWaterBill())) {
            return true;
        }
        if (positive(req.getIndividualRatePerUnit()) || positive(req.getMixedRatePerUnit()) || positive(req.getMixedFixedPool())) {
            return true;
        }
        if (!CollectionUtils.isEmpty(req.getIndividualRows())) {
            return true;
        }
        return !CollectionUtils.isEmpty(req.getMixedMeterRows());
    }

    /**
     * When updating an existing row, if the request did not supply shared expenses (all absent) and the
     * computed per-flat base is zero, reuse the non-water, non-appliances portion of the stored amount.
     */
    public static BigDecimal resolveEffectiveBase(
            BigDecimal incomingBasePerFlat,
            boolean existingRow,
            BigDecimal oldAmount,
            BigDecimal oldWater,
            BigDecimal oldAppliances,
            boolean sharedExpensePayloadPresent) {
        BigDecimal base = Objects.requireNonNullElse(incomingBasePerFlat, ZERO);
        if (!existingRow || base.compareTo(ZERO) > 0 || sharedExpensePayloadPresent) {
            return base;
        }
        BigDecimal preserved = nz(oldAmount).subtract(nz(oldWater)).subtract(nz(oldAppliances));
        if (preserved.compareTo(ZERO) < 0) {
            preserved = ZERO;
        }
        return preserved;
    }

    /**
     * When the request carries no water inputs and incoming water is zero, keep the stored water amount.
     */
    public static BigDecimal resolveWaterForFlat(
            BigDecimal incomingWater,
            boolean existingRow,
            BigDecimal oldWater,
            boolean waterPayloadPresent) {
        BigDecimal w = Objects.requireNonNullElse(incomingWater, ZERO);
        if (existingRow && !waterPayloadPresent && w.compareTo(ZERO) <= 0) {
            BigDecimal old = nz(oldWater);
            if (old.compareTo(ZERO) > 0) {
                return old;
            }
        }
        return w;
    }

    private static boolean positive(BigDecimal v) {
        return v != null && v.compareTo(ZERO) > 0;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? ZERO : v;
    }
}
