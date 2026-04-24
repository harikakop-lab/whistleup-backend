package com.whistleup.backend.service;

import com.whistleup.backend.constants.RentStatus;
import com.whistleup.backend.entity.Maintenance;
import com.whistleup.backend.entity.Profile;
import com.whistleup.backend.entity.RentPayment;
import com.whistleup.backend.repository.MaintenanceRepository;
import com.whistleup.backend.repository.ProfileRepository;
import com.whistleup.backend.repository.RentPaymentRepository;
import com.whistleup.backend.resource.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final MaintenanceRepository maintenanceRepository;
    private final RentPaymentRepository rentPaymentRepository;
    private final ProfileRepository profileRepository;
    private final LedgerService ledgerService;

    public PaymentProfileSummaryResource getProfileSummary(String profileId, Integer year, String month) {
        Profile profile = profileRepository.findByPhone(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found for id: " + profileId));

        int resolvedYear = year == null ? LocalDate.now().getYear() : year;
        int resolvedMonthValue = resolveMonthValue(month);
        String resolvedMonthName = Month.of(resolvedMonthValue).getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        List<Maintenance> maintenanceRows = maintenanceRepository
                .findByProfileIdOrderByMaintenanceYearDescMaintenanceMonthDesc(profileId);
        List<PaymentCurrentResource> maintenanceHistory = maintenanceRows
                .stream()
                .map(this::toMaintenanceItem)
                .toList();

        PaymentCurrentResource maintenanceCurrent = maintenanceRows.stream()
                .filter(m -> Objects.equals(m.getMaintenanceYear(), resolvedYear))
                .filter(m -> Objects.equals(m.getMaintenanceMonth(), resolvedMonthValue))
                .map(this::toMaintenanceItem)
                .findFirst()
                .orElse(null);

        List<RentPayment> rentRows = rentPaymentRepository
                .findByProfileIdOrderByRentYearDescRentMonthDesc(profileId);
        List<PaymentCurrentResource> rentHistory = rentRows
                .stream()
                .map(this::toRentItem)
                .toList();

        PaymentCurrentResource rentCurrent = rentRows.stream()
                .filter(r -> Objects.equals(r.getRentYear(), resolvedYear))
                .filter(r -> Objects.equals(r.getRentMonth(), resolvedMonthValue))
                .map(this::toRentItem)
                .findFirst()
                .orElse(null);

        return PaymentProfileSummaryResource.builder()
                .profileId(profileId)
                .buildingId(profile.getBuildingId())
                .year(resolvedYear)
                .month(resolvedMonthName)
                .maintenanceCurrent(maintenanceCurrent)
                .maintenanceHistory(maintenanceHistory)
                .rentCurrent(rentCurrent)
                .rentHistory(rentHistory)
                .build();
    }

    public PaymentApartmentSummaryResource getApartmentSummary(String buildingId, Integer year, String month) {
        int resolvedYear = year == null ? LocalDate.now().getYear() : year;
        int resolvedMonthValue = resolveMonthValue(month);
        String resolvedMonthName = Month.of(resolvedMonthValue).getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        LedgerResponse ledgerResponse;
        try {
            ledgerResponse = ledgerService.getLedgerByYearAndMonthAndBuilding(resolvedYear, resolvedMonthName, buildingId);
        } catch (Exception ex) {
            ledgerResponse = new LedgerResponse(
                    null,
                    resolvedYear,
                    resolvedMonthName,
                    0,
                    0,
                    0,
                    List.of()
            );
            ledgerResponse.setBuildingId(buildingId);
            ledgerResponse.setFlatsPaid(0L);
            ledgerResponse.setDueDate(null);
        }

        double totalCollection = ledgerResponse.getTotalAmount();
        long flatsPaid = ledgerResponse.getFlatsPaid() == null ? 0L : ledgerResponse.getFlatsPaid();
        int flatsTotal = ledgerResponse.getTotalFlats();
        double collected = flatsPaid * ledgerResponse.getPerFlatAmount();
        double pending = Math.max(totalCollection - collected, 0);

        return PaymentApartmentSummaryResource.builder()
                .buildingId(buildingId)
                .year(resolvedYear)
                .month(resolvedMonthName)
                .totalCollection(totalCollection)
                .collected(collected)
                .pending(pending)
                .spent(ledgerResponse.getTotalExpenses())
                .flatsPaid(flatsPaid)
                .flatsTotal(flatsTotal)
                .perFlatAmount(ledgerResponse.getPerFlatAmount())
                .dueDate(ledgerResponse.getDueDate())
                .items(ledgerResponse.getItems() == null ? List.of() : ledgerResponse.getItems())
                .build();
    }

    public PaymentCurrentResource upsertRent(RentUpsertRequest request) {
        String profileId = request.getProfileId();
        String buildingId = request.getBuildingId();
        if (profileId == null || profileId.isBlank()) {
            throw new IllegalArgumentException("profileId is required");
        }
        if (buildingId == null || buildingId.isBlank()) {
            throw new IllegalArgumentException("buildingId is required");
        }
        int year = request.getYear() == null ? LocalDate.now().getYear() : request.getYear();
        int month = request.getMonth() == null ? LocalDate.now().getMonthValue() : request.getMonth();
        BigDecimal amount = request.getAmount() == null ? BigDecimal.ZERO : request.getAmount();
        LocalDate dueDate = request.getDueDate() == null ? LocalDate.now().withDayOfMonth(Math.min(LocalDate.now().lengthOfMonth(), 10)) : request.getDueDate();

        RentPayment entity = rentPaymentRepository.findByProfileIdAndBuildingIdAndRentYearAndRentMonth(
                        profileId, buildingId, year, month
                )
                .orElseGet(() -> RentPayment.builder()
                        .profileId(profileId)
                        .buildingId(buildingId)
                        .rentYear(year)
                        .rentMonth(month)
                        .status(RentStatus.PENDING)
                        .build());

        entity.setAmount(amount);
        entity.setDueDate(dueDate);
        if (entity.getStatus() == null) {
            entity.setStatus(RentStatus.PENDING);
        }
        RentPayment saved = rentPaymentRepository.save(entity);
        return toRentItem(saved);
    }

    public void markRentPaid(Long rentId) {
        RentPayment entity = rentPaymentRepository.findById(rentId)
                .orElseThrow(() -> new IllegalArgumentException("Rent record not found"));
        entity.setStatus(RentStatus.PAID);
        entity.setPaidDate(LocalDate.now());
        rentPaymentRepository.save(entity);
    }

    private PaymentCurrentResource toMaintenanceItem(Maintenance m) {
        String month = Month.of(m.getMaintenanceMonth()).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        return PaymentCurrentResource.builder()
                .id(m.getId())
                .title(month + " Maintenance")
                .amount(m.getAmount())
                .dueDate(m.getDueDate())
                .status(m.getStatus().name())
                .paidDate(m.getPaidDate())
                .build();
    }

    private PaymentCurrentResource toRentItem(RentPayment r) {
        String month = Month.of(r.getRentMonth()).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        return PaymentCurrentResource.builder()
                .id(r.getId())
                .title(month + " Rent")
                .amount(r.getAmount())
                .dueDate(r.getDueDate())
                .status(r.getStatus().name())
                .paidDate(r.getPaidDate())
                .build();
    }

    private int resolveMonthValue(String month) {
        if (month == null || month.isBlank()) {
            return LocalDate.now().getMonthValue();
        }
        String value = month.trim();
        if (value.matches("\\d+")) {
            int numeric = Integer.parseInt(value);
            if (numeric >= 1 && numeric <= 12) return numeric;
        }
        String upper = value.toUpperCase(Locale.ENGLISH);
        if (upper.startsWith("JAN")) return 1;
        if (upper.startsWith("FEB")) return 2;
        if (upper.startsWith("MAR")) return 3;
        if (upper.startsWith("APR")) return 4;
        if (upper.startsWith("MAY")) return 5;
        if (upper.startsWith("JUN")) return 6;
        if (upper.startsWith("JUL")) return 7;
        if (upper.startsWith("AUG")) return 8;
        if (upper.startsWith("SEP")) return 9;
        if (upper.startsWith("OCT")) return 10;
        if (upper.startsWith("NOV")) return 11;
        if (upper.startsWith("DEC")) return 12;
        throw new IllegalArgumentException("Invalid month: " + month);
    }
}
