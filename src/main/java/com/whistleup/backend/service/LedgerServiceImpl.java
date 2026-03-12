package com.whistleup.backend.service;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.whistleup.backend.entity.Ledger;
import com.whistleup.backend.entity.LedgerItem;
import com.whistleup.backend.entity.Maintenance;
import com.whistleup.backend.repository.LedgerRepository;
import com.whistleup.backend.repository.MaintenanceRepository;
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
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@Transactional
public class LedgerServiceImpl implements LedgerService {

    private final LedgerRepository ledgerRepository;

    private final MaintenanceService maintenanceService;

    private final MaintenanceRepository maintenanceRepository;

    public LedgerServiceImpl(
            LedgerRepository ledgerRepository,
            MaintenanceService maintenanceService,
            MaintenanceRepository maintenanceRepository) {
        this.ledgerRepository = ledgerRepository;
        this.maintenanceService = maintenanceService;
        this.maintenanceRepository = maintenanceRepository;
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
        ledger.setTotalFlats(request.getTotalFlats());
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
            maintenanceAmount = BigDecimal.valueOf(totalMaintenanceAmount / request.getTotalFlats());
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
        Optional<Ledger> existing = ledgerRepository.findByYearAndMonthAndBuildingIdWithItems(
                year,
                normalizeMonth(month),
                buildingId
        );
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }
        List<Maintenance> maintenanceRows = maintenanceRepository.findByBuildingIdAndMaintenanceYearAndMaintenanceMonth(
                buildingId,
                year,
                getMonthValue(month)
        );
        if (maintenanceRows.isEmpty()) {
            throw new IllegalStateException("No ledger/maintenance found for requested month");
        }
        LedgerResponse response = new LedgerResponse(
                null,
                year,
                normalizeMonth(month),
                maintenanceRows.stream().mapToDouble(m -> m.getAmount().doubleValue()).sum(),
                maintenanceRows.size(),
                maintenanceRows.stream().mapToDouble(m -> m.getAmount().doubleValue()).average().orElse(0.0),
                List.of()
        );
        response.setBuildingId(buildingId);
        response.setFlatsPaid(maintenanceRows.stream().filter(m -> m.getStatus().name().equals("PAID")).count());
        response.setDueDate(maintenanceRows.stream()
                .map(Maintenance::getDueDate)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null));
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
            ledgerResponse.setFlatsPaid(rows.stream().filter(m -> m.getStatus().name().equals("PAID")).count());
            ledgerResponse.setDueDate(rows.stream().map(Maintenance::getDueDate).findFirst().orElse(null));
        }
        return ledgerResponse;
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
