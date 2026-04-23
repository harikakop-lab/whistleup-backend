package com.whistleup.backend.service;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.whistleup.backend.entity.BuildingDetails;
import com.whistleup.backend.entity.Ledger;
import com.whistleup.backend.entity.LedgerItem;
import com.whistleup.backend.entity.Maintenance;
import com.whistleup.backend.entity.Profile;
import com.whistleup.backend.repository.BuildingDetailsRepository;
import com.whistleup.backend.repository.LedgerRepository;
import com.whistleup.backend.repository.MaintenanceRepository;
import com.whistleup.backend.repository.ProfileRepository;
import com.whistleup.backend.resource.*;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class LedgerServiceImpl implements LedgerService {

    private final LedgerRepository ledgerRepository;

    private final MaintenanceService maintenanceService;

    private final MaintenanceRepository maintenanceRepository;

    private final ProfileRepository profileRepository;

    private final BuildingDetailsRepository buildingDetailsRepository;

    public LedgerServiceImpl(
            LedgerRepository ledgerRepository,
            MaintenanceService maintenanceService,
            MaintenanceRepository maintenanceRepository,
            ProfileRepository profileRepository,
            BuildingDetailsRepository buildingDetailsRepository) {
        this.ledgerRepository = ledgerRepository;
        this.maintenanceService = maintenanceService;
        this.maintenanceRepository = maintenanceRepository;
        this.profileRepository = profileRepository;
        this.buildingDetailsRepository = buildingDetailsRepository;
    }

    @Override
    public LedgerResponse createLedger(CreateLedgerRequest request) {
        Ledger ledger = ledgerRepository.findByYearAndMonthAndBuildingId(
                request.getYear(),
                normalizeMonth(request.getMonth()),
                request.getBuildingId()
        ).orElseGet(Ledger::new);
        ledger.setYear(request.getYear());
        ledger.setMonth(normalizeMonth(request.getMonth()));
        int residentCount = resolveTotalResidentsForBuilding(request.getBuildingId(), request.getTotalFlats());
        ledger.setTotalFlats(residentCount);
        ledger.setCreatedAt(ledger.getCreatedAt() == null ? LocalDateTime.now() : ledger.getCreatedAt());
        ledger.setBuildingId(request.getBuildingId());
        ledger.getItems().clear();
        mapItems(request.getItems(), ledger);

        calculateTotals(ledger);
        MaintenanceCreateResource maintenanceCreateResource = MaintenanceCreateResource.builder().build();
        maintenanceCreateResource.setYear(request.getYear());
        maintenanceCreateResource.setMonth(getMonthValue(request.getMonth()));
        BigDecimal maintenanceAmount = new BigDecimal(0);
        if (!CollectionUtils.isEmpty(request.getItems())) {
            Double totalMaintenanceAmount = request.getItems().stream().mapToDouble(LedgerItemRequest::getAmount).sum();
            maintenanceAmount = BigDecimal.valueOf(totalMaintenanceAmount / residentCount);
        }
        maintenanceCreateResource.setAmount(maintenanceAmount);
        maintenanceCreateResource.setDueDate(YearMonth.now().atEndOfMonth());
        maintenanceCreateResource.setBuildingId(request.getBuildingId());
        maintenanceService.createOrUpdateMaintenance(maintenanceCreateResource);
        Ledger savedLedger = ledgerRepository.save(ledger);
        return toResponse(savedLedger);
    }

    private Integer getMonthValue(@NotBlank String month) {
        String normalized = normalizeMonth(month).toUpperCase(Locale.ENGLISH);
        return switch (normalized) {
            case "JANUARY" -> 1;
            case "FEBRUARY" -> 2;
            case "MARCH" -> 3;
            case "APRIL" -> 4;
            case "MAY" -> 5;
            case "JUNE" -> 6;
            case "JULY" -> 7;
            case "AUGUST" -> 8;
            case "SEPTEMBER" -> 9;
            case "OCTOBER" -> 10;
            case "NOVEMBER" -> 11;
            case "DECEMBER" -> 12;
            default -> throw new IllegalStateException("Unexpected value: " + normalized);
        };
    }

    @Override
    public LedgerResponse getLedgerByYearAndMonth(int year, String month) {
        Ledger ledger = ledgerRepository
                .findTopByYearAndMonthOrderByIdDesc(year, normalizeMonth(month))
                .orElseThrow();
        return toResponse(ledger);
    }

    @Override
    public LedgerResponse getLedgerByYearAndMonthAndBuilding(int year, String month, String buildingId) {
        List<Maintenance> maintenanceRows = maintenanceRepository.findByBuildingIdAndMaintenanceYearAndMaintenanceMonth(
                buildingId,
                year,
                getMonthValue(month)
        );
        Optional<Ledger> existing = ledgerRepository.findByYearAndMonthAndBuildingIdWithItems(
                year,
                normalizeMonth(month),
                buildingId
        );
        if (existing.isPresent()) {
            LedgerResponse response = toResponse(existing.get());
            if (!maintenanceRows.isEmpty()) {
                enrichFromMaintenanceRows(response, maintenanceRows);
            }
            return response;
        }
        if (maintenanceRows.isEmpty()) {
            throw new IllegalStateException("No ledger/maintenance found for requested month");
        }
        LedgerResponse response = new LedgerResponse(
                null,
                year,
                normalizeMonth(month),
                0,
                0,
                0,
                buildItemsFromMaintenanceRows(maintenanceRows)
        );
        response.setBuildingId(buildingId);
        enrichFromMaintenanceRows(response, maintenanceRows);
        return response;
    }

    @Override
    public LedgerResponse updateLedger(Long ledgerId, UpdateLedgerRequest request) {
        Ledger ledger = ledgerRepository.findById(ledgerId).orElseThrow();
        ledger.getItems().clear();
        mapItems(request.getItems(), ledger);
        calculateTotals(ledger);
        ledger.setUpdatedAt(LocalDateTime.now());
        ledger.setBuildingId(request.getBuildingId());
        return toResponse(ledgerRepository.save(ledger));
    }

    @Override
    public byte[] generateLedgerPdf(Long ledgerId) {
        Ledger ledger = ledgerRepository.findByIdWithItems(ledgerId).orElseThrow();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();

        PdfWriter.getInstance(document, out);
        document.open();

        document.add(new Paragraph(ledger.getMonth() + " " + ledger.getYear() + " - " + ledger.getBuildingId()));
        document.add(new Paragraph(" "));

        ledger.getItems().forEach(item ->
                document.add(new Paragraph(item.getName() + " : ₹" + item.getAmount()))
        );

        document.add(new Paragraph(" "));
        document.add(new Paragraph("Total Amount: ₹" + ledger.getTotalAmount()));
        document.add(new Paragraph("Total Flats: " + ledger.getTotalFlats()));
        document.add(new Paragraph("Per Flat: ₹" + ledger.getPerFlatAmount()));

        document.close();
        return out.toByteArray();
    }

    private void mapItems(List<LedgerItemRequest> items, Ledger ledger) {
        for (LedgerItemRequest req : items) {
            LedgerItem item = new LedgerItem();
            item.setName(req.getName());
            item.setAmount(req.getAmount());
            item.setLedger(ledger);
            ledger.getItems().add(item);
        }
    }

    private void calculateTotals(Ledger ledger) {
        double total = ledger.getItems()
                .stream()
                .mapToDouble(LedgerItem::getAmount)
                .sum();
        ledger.setTotalAmount(total);
        ledger.setPerFlatAmount(
                ledger.getTotalFlats() == 0 ? 0 : total / ledger.getTotalFlats()
        );
    }

    private LedgerResponse toResponse(Ledger ledger) {
        List<LedgerItemResponse> items = ledger.getItems()
                .stream()
                .map(i -> new LedgerItemResponse(i.getId(), i.getName(), i.getAmount()))
                .toList();

        LedgerResponse ledgerResponse = new LedgerResponse(
                ledger.getId(),
                ledger.getYear(),
                ledger.getMonth(),
                ledger.getTotalAmount(),
                ledger.getTotalFlats(),
                ledger.getPerFlatAmount(),
                items
        );
        ledgerResponse.setBuildingId(ledger.getBuildingId());
        List<Maintenance> rows = maintenanceRepository.findByBuildingIdAndMaintenanceYearAndMaintenanceMonth(
                ledger.getBuildingId(),
                ledger.getYear(),
                getMonthValue(ledger.getMonth())
        );
        if (!rows.isEmpty()) {
            enrichFromMaintenanceRows(ledgerResponse, rows);
            if (CollectionUtils.isEmpty(ledgerResponse.getItems())) {
                ledgerResponse.setItems(buildItemsFromMaintenanceRows(rows));
            }
        }
        return ledgerResponse;
    }

    private void enrichFromMaintenanceRows(LedgerResponse response, List<Maintenance> rows) {
        double totalAmount = rows.stream().mapToDouble(m -> m.getAmount().doubleValue()).sum();
        int totalFlats = resolveTotalResidentsForBuilding(response.getBuildingId(), rows.size());
        response.setTotalAmount(totalAmount);
        response.setTotalFlats(totalFlats);
        response.setPerFlatAmount(totalFlats == 0 ? 0 : totalAmount / totalFlats);
        response.setFlatsPaid(rows.stream().filter(m -> "PAID".equals(m.getStatus().name())).count());
        response.setDueDate(rows.stream()
                .map(Maintenance::getDueDate)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null));
    }

    private int resolveTotalResidentsForBuilding(String buildingId, int fallbackCount) {
        if (buildingId == null || buildingId.isBlank()) {
            return Math.max(fallbackCount, 1);
        }
        try {
            BuildingDetails buildingDetails = buildingDetailsRepository.findById(Long.valueOf(buildingId.trim())).orElse(null);
            if (buildingDetails != null && buildingDetails.getTotalResidents() != null && buildingDetails.getTotalResidents() > 0) {
                return Math.toIntExact(buildingDetails.getTotalResidents());
            }
        } catch (Exception ignored) {
            // fall back to existing behavior when building metadata is unavailable
        }
        return Math.max(fallbackCount, 1);
    }

    private List<LedgerItemResponse> buildItemsFromMaintenanceRows(List<Maintenance> rows) {
        if (CollectionUtils.isEmpty(rows)) {
            return List.of();
        }
        double watchman = rows.stream().map(Maintenance::getWatchmanSalary)
                .filter(java.util.Objects::nonNull).mapToDouble(BigDecimal::doubleValue).sum();
        double garbage = rows.stream().map(Maintenance::getGarbageCollection)
                .filter(java.util.Objects::nonNull).mapToDouble(BigDecimal::doubleValue).sum();
        double lift = rows.stream().map(Maintenance::getLiftMaintenance)
                .filter(java.util.Objects::nonNull).mapToDouble(BigDecimal::doubleValue).sum();
        double electricity = rows.stream().map(Maintenance::getElectricityCommon)
                .filter(java.util.Objects::nonNull).mapToDouble(BigDecimal::doubleValue).sum();
        double motor = rows.stream().map(Maintenance::getMotorPump)
                .filter(java.util.Objects::nonNull).mapToDouble(BigDecimal::doubleValue).sum();
        double misc = rows.stream().map(Maintenance::getMiscellaneous)
                .filter(java.util.Objects::nonNull).mapToDouble(BigDecimal::doubleValue).sum();
        double water = rows.stream().map(Maintenance::getWaterAmount)
                .filter(java.util.Objects::nonNull).mapToDouble(BigDecimal::doubleValue).sum();
        Map<String, Double> customExpenses = new LinkedHashMap<>();
        for (Maintenance row : rows) {
            if (row.getCustomExpenses() == null || row.getCustomExpenses().isEmpty()) {
                continue;
            }
            for (Map.Entry<String, BigDecimal> entry : row.getCustomExpenses().entrySet()) {
                if (entry.getKey() == null || entry.getKey().trim().isEmpty() || entry.getValue() == null) {
                    continue;
                }
                customExpenses.merge(entry.getKey().trim(), entry.getValue().doubleValue(), Double::sum);
            }
        }

        List<LedgerItemResponse> items = new ArrayList<>();
        if (watchman > 0) items.add(new LedgerItemResponse(null, "Watchman Salary", watchman));
        if (garbage > 0) items.add(new LedgerItemResponse(null, "Garbage Collection", garbage));
        if (lift > 0) items.add(new LedgerItemResponse(null, "Lift Maintenance", lift));
        if (electricity > 0) items.add(new LedgerItemResponse(null, "Common Area Electricity", electricity));
        if (motor > 0) items.add(new LedgerItemResponse(null, "Motor Maintenance", motor));
        if (misc > 0) items.add(new LedgerItemResponse(null, "Miscellaneous", misc));
        customExpenses.forEach((name, amount) -> {
            if (amount > 0) {
                items.add(new LedgerItemResponse(null, name, amount));
            }
        });
        if (water > 0) items.add(new LedgerItemResponse(null, "Water Charges", water));

        double appliancesTotal = rows.stream()
                .map(Maintenance::getAppliancesAmount)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .sum();
        if (appliancesTotal > 0) {
            List<String> flats = new ArrayList<>();
            for (Maintenance row : rows) {
                if (row.getAppliancesAmount() == null || row.getAppliancesAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                String flatLabel = profileRepository.findByPhone(row.getProfileId())
                        .map(Profile::getFlatNo)
                        .filter(f -> f != null && !f.isBlank())
                        .orElse(row.getProfileId());
                flats.add(flatLabel);
            }
            String label = "Appliances - " + String.join(", ", flats);
            items.add(new LedgerItemResponse(null, label, appliancesTotal));
        }
        return items;
    }

    private String normalizeMonth(String month) {
        String value = month == null ? "" : month.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("month is required");
        }
        String upper = value.toUpperCase(Locale.ENGLISH);
        try {
            return java.time.Month.valueOf(upper).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        } catch (Exception e) {
            try {
                return java.time.Month.valueOf(upper + "UARY").getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            } catch (Exception ignored) {
                if (upper.startsWith("JAN")) return "January";
                if (upper.startsWith("FEB")) return "February";
                if (upper.startsWith("MAR")) return "March";
                if (upper.startsWith("APR")) return "April";
                if (upper.startsWith("MAY")) return "May";
                if (upper.startsWith("JUN")) return "June";
                if (upper.startsWith("JUL")) return "July";
                if (upper.startsWith("AUG")) return "August";
                if (upper.startsWith("SEP")) return "September";
                if (upper.startsWith("OCT")) return "October";
                if (upper.startsWith("NOV")) return "November";
                if (upper.startsWith("DEC")) return "December";
                throw new IllegalArgumentException("Invalid month: " + month);
            }
        }
    }
}
