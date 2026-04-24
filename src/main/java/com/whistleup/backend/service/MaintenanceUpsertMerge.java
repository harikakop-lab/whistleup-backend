package com.whistleup.backend.service;

import com.whistleup.backend.resource.MaintenanceCreateResource;
import com.whistleup.backend.resource.MaintenanceMeterRowResource;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Merge rules when re-saving maintenance for the same profile/month so partial payloads
 * (e.g. water-only) do not wipe an existing base charge.
 *
 * <p>Clients often send {@code BigDecimal.ZERO} for every empty expense field (not JSON omission).
 * Those zeros must <strong>not</strong> be treated as a "full shared expense resubmission", or the
 * preserved-base path is skipped incorrectly.
 */
public final class MaintenanceUpsertMerge {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private MaintenanceUpsertMerge() {
    }

    /**
     * True when the request carries a <em>positive</em> shared-expense total (standard lines + custom map),
     * mirroring the additive part of {@code MaintenanceService.resolveSharedExpenseTotal} without the
     * legacy {@code amount * totalFlats} fallback.
     */
    public static boolean hasPositiveSharedExpenseSum(MaintenanceCreateResource req) {
        if (req == null) {
            return false;
        }
        BigDecimal total = ZERO;
        total = total.add(nz(req.getWatchmanSalary()));
        total = total.add(nz(req.getGarbageCollection()));
        total = total.add(nz(req.getLiftMaintenance()));
        total = total.add(nz(req.getElectricityCommon()));
        total = total.add(nz(req.getMotorPump()));
        total = total.add(nz(req.getMiscellaneous()));
        Map<String, BigDecimal> custom = req.getCustomExpenses();
        if (custom != null && !custom.isEmpty()) {
            for (BigDecimal v : custom.values()) {
                BigDecimal a = nz(v);
                if (a.compareTo(ZERO) > 0) {
                    total = total.add(a);
                }
            }
        }
        return total.compareTo(ZERO) > 0;
    }

    /**
     * When true, {@link #resolveEffectiveBase} must not reuse the prior row's base (admin is explicitly
     * re-specifying shared expenses or resetting them).
     */
    public static boolean blocksPreservedSharedBaseMerge(MaintenanceCreateResource req) {
        if (req == null) {
            return false;
        }
        if (Boolean.TRUE.equals(req.getResetSharedExpenses())) {
            return true;
        }
        if (hasPositiveSharedExpenseSum(req)) {
            return true;
        }
        // Legacy path: total shared derived from per-flat amount * flats when line items are all zero
        if (req.getAmount() != null && req.getAmount().compareTo(ZERO) > 0) {
            return true;
        }
        return false;
    }

    /**
     * True only when the request supplies actual water <em>amounts</em> (bill or metered usage).
     * Sending only {@code waterMode} (e.g. {@code FIXED}) without a bill must return false so we keep
     * the existing per-row {@code waterAmount} from the database on merge.
     */
    public static boolean hasWaterPayload(MaintenanceCreateResource req) {
        if (req == null) {
            return false;
        }
        if (positive(req.getFixedWaterBill()) || positive(req.getMasterWaterBill())) {
            return true;
        }
        if (positive(req.getMixedFixedPool())) {
            return true;
        }
        if (positive(req.getIndividualRatePerUnit()) && hasPositiveMeterUnits(req.getIndividualRows())) {
            return true;
        }
        if (positive(req.getMixedRatePerUnit()) && hasPositiveMeterUnits(req.getMixedMeterRows())) {
            return true;
        }
        return false;
    }

    private static boolean hasPositiveMeterUnits(List<MaintenanceMeterRowResource> rows) {
        if (CollectionUtils.isEmpty(rows)) {
            return false;
        }
        for (MaintenanceMeterRowResource row : rows) {
            if (row == null) {
                continue;
            }
            BigDecimal units = row.getUnits();
            if (units != null && units.compareTo(ZERO) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * When updating an existing row, if the request did not supply positive shared expenses (and did not
     * block merge) and the computed per-flat base is zero, reuse the non-water, non-appliances portion of
     * the stored amount.
     */
    public static BigDecimal resolveEffectiveBase(
            BigDecimal incomingBasePerFlat,
            boolean existingRow,
            BigDecimal oldAmount,
            BigDecimal oldWater,
            BigDecimal oldAppliances,
            boolean blocksPreservedBaseMerge) {
        BigDecimal base = Objects.requireNonNullElse(incomingBasePerFlat, ZERO);
        if (!existingRow || base.compareTo(ZERO) > 0 || blocksPreservedBaseMerge) {
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

    /**
     * True when the request includes at least one positive custom expense total (building-level map).
     * When false on an update, callers should keep the row's existing {@code customExpenses} map.
     */
    public static boolean hasCustomExpensePayload(MaintenanceCreateResource req) {
        if (req == null || req.getCustomExpenses() == null || req.getCustomExpenses().isEmpty()) {
            return false;
        }
        for (BigDecimal v : req.getCustomExpenses().values()) {
            if (positive(v)) {
                return true;
            }
        }
        return false;
    }

    private static boolean positive(BigDecimal v) {
        return v != null && v.compareTo(ZERO) > 0;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? ZERO : v;
    }
}
