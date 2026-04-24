package com.whistleup.backend.service;

import com.whistleup.backend.resource.MaintenanceCreateResource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MaintenanceUpsertMergeTest {

    @Test
    void resolveEffectiveBase_secondSaveWaterOnly_preservesBaseFromExistingRow() {
        BigDecimal basePerFlat = BigDecimal.ZERO;
        boolean existingRow = true;
        BigDecimal oldAmount = new BigDecimal("1000.00");
        BigDecimal oldWater = BigDecimal.ZERO;
        BigDecimal oldAppliances = BigDecimal.ZERO;
        boolean sharedPayload = MaintenanceUpsertMerge.hasSharedExpensePayload(emptyExpenseRequest());

        BigDecimal effective = MaintenanceUpsertMerge.resolveEffectiveBase(
                basePerFlat,
                existingRow,
                oldAmount,
                oldWater,
                oldAppliances,
                sharedPayload);

        assertThat(effective).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    void resolveWaterForFlat_preservesOldWaterWhenNoWaterPayload() {
        BigDecimal incoming = BigDecimal.ZERO;
        BigDecimal oldWater = new BigDecimal("50.00");
        boolean waterPayload = MaintenanceUpsertMerge.hasWaterPayload(new MaintenanceCreateResource());

        BigDecimal w = MaintenanceUpsertMerge.resolveWaterForFlat(incoming, true, oldWater, waterPayload);
        assertThat(w).isEqualByComparingTo(oldWater);
    }

    @Test
    void resolveWaterForFlat_usesIncomingWhenWaterPayloadPresent() {
        MaintenanceCreateResource req = new MaintenanceCreateResource();
        req.setWaterMode("FIXED");
        req.setFixedWaterBill(new BigDecimal("1000"));

        BigDecimal incoming = new BigDecimal("125.00");
        BigDecimal oldWater = new BigDecimal("50.00");
        boolean waterPayload = MaintenanceUpsertMerge.hasWaterPayload(req);

        BigDecimal w = MaintenanceUpsertMerge.resolveWaterForFlat(incoming, true, oldWater, waterPayload);
        assertThat(w).isEqualByComparingTo(incoming);
    }

    @Test
    void combinedBasePlusWater_matchesIncrementalTotal() {
        BigDecimal effectiveBase = MaintenanceUpsertMerge.resolveEffectiveBase(
                BigDecimal.ZERO,
                true,
                new BigDecimal("1000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                false);
        BigDecimal water = new BigDecimal("125.00");
        assertThat(effectiveBase.add(water)).isEqualByComparingTo(new BigDecimal("1125.00"));
    }

    @Test
    void hasSharedExpensePayload_detectsExplicitZeroExpenseField() {
        MaintenanceCreateResource req = new MaintenanceCreateResource();
        req.setWatchmanSalary(BigDecimal.ZERO);
        assertThat(MaintenanceUpsertMerge.hasSharedExpensePayload(req)).isTrue();
    }

    private static MaintenanceCreateResource emptyExpenseRequest() {
        return new MaintenanceCreateResource();
    }
}
