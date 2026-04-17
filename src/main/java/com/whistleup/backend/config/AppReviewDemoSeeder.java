package com.whistleup.backend.config;

import com.whistleup.backend.constants.MaintenanceStatus;
import com.whistleup.backend.constants.RentStatus;
import com.whistleup.backend.constants.Roles;
import com.whistleup.backend.entity.BuildingDetails;
import com.whistleup.backend.entity.Maintenance;
import com.whistleup.backend.entity.Profile;
import com.whistleup.backend.entity.RentPayment;
import com.whistleup.backend.repository.BuildingDetailsRepository;
import com.whistleup.backend.repository.MaintenanceRepository;
import com.whistleup.backend.repository.ProfileRepository;
import com.whistleup.backend.repository.RentPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AppReviewDemoSeeder {

    private final ProfileRepository profileRepository;
    private final BuildingDetailsRepository buildingDetailsRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final RentPaymentRepository rentPaymentRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.review-demo.enabled:false}")
    private boolean reviewDemoEnabled;

    @Value("${app.review-demo.admin-phone:9666499643}")
    private String adminPhone;

    @Value("${app.review-demo.admin-pin:1234}")
    private String adminPin;

    @Value("${app.review-demo.tenant-phone:7036797788}")
    private String tenantPhone;

    @Value("${app.review-demo.tenant-pin:1234}")
    private String tenantPin;

    @Value("${app.review-demo.shared-upi-id:manojchow72@axl}")
    private String reviewUpiId;

    @Bean
    public CommandLineRunner seedReviewDemoAccounts() {
        return args -> {
            if (!reviewDemoEnabled) {
                return;
            }
            if (isBlank(adminPhone) || isBlank(adminPin) || isBlank(tenantPhone) || isBlank(tenantPin)) {
                log.warn("Review demo seeding skipped: missing admin/tenant credentials.");
                return;
            }

            Optional<BuildingDetails> firstBuildingOpt = buildingDetailsRepository.findAll().stream().findFirst();
            if (firstBuildingOpt.isEmpty()) {
                log.warn("Review demo seeding skipped: no building found.");
                return;
            }

            BuildingDetails building = firstBuildingOpt.get();
            String buildingId = String.valueOf(building.getBuildingId());
            LocalDate now = LocalDate.now();

            Profile admin = upsertProfile(
                    adminPhone.trim(),
                    "App Review Admin",
                    "appreview.admin@nestiti.com",
                    adminPin.trim(),
                    Roles.ADMIN,
                    buildingId,
                    "101",
                    true,
                    reviewUpiId
            );
            Profile tenant = upsertProfile(
                    tenantPhone.trim(),
                    "App Review Tenant",
                    "appreview.tenant@nestiti.com",
                    tenantPin.trim(),
                    Roles.USER,
                    buildingId,
                    "202",
                    true,
                    reviewUpiId
            );

            building.setAdminPhone(admin.getPhone());
            building.setAdminName(admin.getName());
            if (!isBlank(reviewUpiId)) {
                building.setUpiId(reviewUpiId.trim());
            }
            buildingDetailsRepository.save(building);

            upsertMaintenance(tenant.getPhone(), buildingId, now.getYear(), now.getMonthValue(), new BigDecimal("2500"));
            upsertRent(tenant.getPhone(), buildingId, now.getYear(), now.getMonthValue(), new BigDecimal("12000"));
            log.info("Review demo accounts seeded successfully for building {}", buildingId);
        };
    }

    private Profile upsertProfile(
            String phone,
            String name,
            String email,
            String plainPin,
            Roles role,
            String buildingId,
            String flatNo,
            boolean assigned,
            String upiId
    ) {
        Profile profile = profileRepository.findByPhone(phone).orElseGet(Profile::new);
        profile.setPhone(phone);
        profile.setName(name);
        profile.setEmail(email);
        profile.setRole(role);
        profile.setPin(passwordEncoder.encode(plainPin));
        profile.setBuildingId(buildingId);
        profile.setFlatNo(flatNo);
        profile.setIsAssigned(assigned);
        if (!isBlank(upiId)) {
            profile.setUpiId(upiId.trim());
        }
        return profileRepository.save(profile);
    }

    private void upsertMaintenance(
            String profileId,
            String buildingId,
            int year,
            int month,
            BigDecimal amount
    ) {
        Maintenance maintenance = maintenanceRepository
                .findByProfileIdAndBuildingIdAndMaintenanceYearAndMaintenanceMonth(profileId, buildingId, year, month)
                .orElseGet(Maintenance::new);
        maintenance.setProfileId(profileId);
        maintenance.setBuildingId(buildingId);
        maintenance.setMaintenanceYear(year);
        maintenance.setMaintenanceMonth(month);
        maintenance.setAmount(amount);
        maintenance.setDueDate(LocalDate.now().plusDays(5));
        maintenance.setStatus(MaintenanceStatus.PENDING);
        maintenanceRepository.save(maintenance);
    }

    private void upsertRent(
            String profileId,
            String buildingId,
            int year,
            int month,
            BigDecimal amount
    ) {
        RentPayment rent = rentPaymentRepository
                .findByProfileIdAndBuildingIdAndRentYearAndRentMonth(profileId, buildingId, year, month)
                .orElseGet(RentPayment::new);
        rent.setProfileId(profileId);
        rent.setBuildingId(buildingId);
        rent.setRentYear(year);
        rent.setRentMonth(month);
        rent.setAmount(amount);
        rent.setDueDate(LocalDate.now().plusDays(5));
        rent.setStatus(RentStatus.PENDING);
        rentPaymentRepository.save(rent);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
