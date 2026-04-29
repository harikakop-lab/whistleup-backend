package com.whistleup.backend.service;

import com.whistleup.backend.entity.Maintenance;
import com.whistleup.backend.resource.MaintenanceCreateResource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MaintenanceServiceAssetMergeTest {

    @Test
    void resolveAssetAmountToPersist_addsIncomingToExistingAmount() {
        MaintenanceCreateResource req = new MaintenanceCreateResource();
        req.setAssetAmount(new BigDecimal("1000.00"));
        Maintenance existing = Maintenance.builder()
                .assetAmount(new BigDecimal("20000.00"))
                .build();

        BigDecimal merged = MaintenanceService.resolveAssetAmountToPersist(req, Optional.of(existing));

        assertThat(merged).isEqualByComparingTo(new BigDecimal("21000.00"));
    }

    @Test
    void resolveAssetAmountToPersist_usesIncomingWhenNoExistingRow() {
        MaintenanceCreateResource req = new MaintenanceCreateResource();
        req.setAssetAmount(new BigDecimal("20000.00"));

        BigDecimal merged = MaintenanceService.resolveAssetAmountToPersist(req, Optional.empty());

        assertThat(merged).isEqualByComparingTo(new BigDecimal("20000.00"));
    }

    @Test
    void resolveAssetDescriptionToPersist_keepsExistingWhenIncomingBlank() {
        MaintenanceCreateResource req = new MaintenanceCreateResource();
        req.setAssetDescription("   ");
        Maintenance existing = Maintenance.builder()
                .assetDescription("Shutters")
                .build();

        String merged = MaintenanceService.resolveAssetDescriptionToPersist(req, Optional.of(existing));

        assertThat(merged).isEqualTo("Shutters");
    }
}
