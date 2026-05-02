package com.whistleup.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.whistleup.backend.entity.BuildingDetails;
import com.whistleup.backend.entity.Ledger;
import com.whistleup.backend.entity.LedgerItem;
import com.whistleup.backend.entity.Maintenance;
import com.whistleup.backend.entity.NotebookMonthly;
import com.whistleup.backend.repository.BuildingDetailsRepository;
import com.whistleup.backend.repository.LedgerRepository;
import com.whistleup.backend.repository.MaintenanceRepository;
import com.whistleup.backend.resource.*;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
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

    private final BuildingDetailsRepository buildingDetailsRepository;
    private final NotebookService notebookService;
    private final ObjectMapper objectMapper;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    public LedgerServiceImpl(
            LedgerRepository ledgerRepository,
            MaintenanceService maintenanceService,
            MaintenanceRepository maintenanceRepository,
            BuildingDetailsRepository buildingDetailsRepository,
            NotebookService notebookService,
            ObjectMapper objectMapper) {
        this.ledgerRepository = ledgerRepository;
        this.maintenanceService = maintenanceService;
        this.maintenanceRepository = maintenanceRepository;
        this.buildingDetailsRepository = buildingDetailsRepository;
        this.notebookService = notebookService;
        this.objectMapper = objectMapper;
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
        if (maintenanceRows.isEmpty()) {
            throw new IllegalStateException("No ledger/maintenance found for requested month");
        }
        String normalizedMonth = normalizeMonth(month);
        Optional<Ledger> existing = ledgerRepository.findByYearAndMonthAndBuildingIdWithItems(
                year,
                normalizedMonth,
                buildingId
        );
        return composeLedgerForBuilding(
                buildingId,
                year,
                normalizedMonth,
                existing.map(Ledger::getId).orElse(null),
                maintenanceRows
        );
    }

    /**
     * Single source of truth for building-scoped ledger: maintenance rows define collected amounts and
     * paid-flat progress; notebook (if any) supplies opening-balance toggle, expenses, and closing math.
     */
    private LedgerResponse composeLedgerForBuilding(
            String buildingId,
            int year,
            String normalizedMonth,
            Long persistedLedgerId,
            List<Maintenance> maintenanceRows) {
        Optional<NotebookMonthly> notebook = notebookService.findNotebookEntity(buildingId, year, normalizedMonth);

        List<LedgerItemResponse> items = new ArrayList<>(buildItemsFromMaintenanceRows(maintenanceRows));
        if (notebook.isPresent()) {
            items.addAll(buildItemsFromNotebook(notebook.get()));
        }

        LedgerResponse response = new LedgerResponse(
                persistedLedgerId,
                year,
                normalizedMonth,
                0,
                0,
                0,
                items
        );
        response.setBuildingId(buildingId);
        response.setItems(items);
        response.setFlatsPaid(maintenanceRows.stream().filter(m -> "PAID".equals(m.getStatus().name())).count());
        response.setDueDate(maintenanceRows.stream()
                .map(Maintenance::getDueDate)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null));
        if (notebook.isPresent()) {
            applyNotebookSnapshot(response, notebook.get());
        } else {
            response.setOpeningBalance(0);
            response.setTotalExpenses(0);
        }
        enrichFromMaintenanceRows(response, maintenanceRows);
        response.setAttachments(buildLedgerAttachmentResponses(buildingId, year, maintenanceRows));
        return response;
    }

    private List<LedgerAttachmentResponse> buildLedgerAttachmentResponses(
            String buildingId, int year, List<Maintenance> maintenanceRows) {
        if (CollectionUtils.isEmpty(maintenanceRows)) {
            return List.of();
        }
        String json = maintenanceRows.stream()
                .map(Maintenance::getLedgerAttachmentsJson)
                .filter(s -> s != null && !s.isBlank())
                .findFirst()
                .orElse(null);
        if (json == null) {
            return List.of();
        }
        try {
            List<LedgerAttachmentStored> stored =
                    objectMapper.readValue(json, new TypeReference<List<LedgerAttachmentStored>>() {});
            if (stored == null || stored.isEmpty()) {
                return List.of();
            }
            int month = maintenanceRows.get(0).getMaintenanceMonth();
            String base = normalizedBaseUrl();
            List<LedgerAttachmentResponse> out = new ArrayList<>();
            for (LedgerAttachmentStored s : stored) {
                if (s.getFileName() == null || s.getFileName().isBlank()) {
                    continue;
                }
                String url = base + "/whistleup/maintenance/ledger-attachment/"
                        + buildingId
                        + "/"
                        + year
                        + "/"
                        + month
                        + "/"
                        + s.getFileName();
                out.add(
                        LedgerAttachmentResponse.builder()
                                .url(url)
                                .contentType(s.getContentType())
                                .fileName(s.getFileName())
                                .build());
            }
            return out;
        } catch (Exception e) {
            return List.of();
        }
    }

    private String normalizedBaseUrl() {
        String b = appBaseUrl == null ? "" : appBaseUrl.trim();
        if (b.endsWith("/")) {
            return b.substring(0, b.length() - 1);
        }
        return b;
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
            item.setSectionKey(req.getSectionKey());
            item.setSectionLabel(req.getSectionLabel());
            item.setLedger(ledger);
            ledger.getItems().add(item);
        }
    }

    private void calculateTotals(Ledger ledger) {
        boolean hasCollectionSection = ledger.getItems().stream()
                .anyMatch(item -> "COLLECTION".equalsIgnoreCase(item.getSectionKey()));
        double collectionTotal = ledger.getItems()
                .stream()
                .filter(item -> !hasCollectionSection || "COLLECTION".equalsIgnoreCase(item.getSectionKey()))
                .mapToDouble(LedgerItem::getAmount)
                .sum();
        double expenseTotal = ledger.getItems()
                .stream()
                .filter(item -> "EXPENSE".equalsIgnoreCase(item.getSectionKey()))
                .mapToDouble(LedgerItem::getAmount)
                .sum();
        ledger.setTotalAmount(collectionTotal);
        ledger.setTotalExpenses(expenseTotal);
        ledger.setPerFlatAmount(
                ledger.getTotalFlats() == 0 ? 0 : collectionTotal / ledger.getTotalFlats()
        );
    }

    private LedgerResponse toResponse(Ledger ledger) {
        if (ledger.getBuildingId() != null && !ledger.getBuildingId().isBlank()) {
            List<Maintenance> rows = maintenanceRepository.findByBuildingIdAndMaintenanceYearAndMaintenanceMonth(
                    ledger.getBuildingId(),
                    ledger.getYear(),
                    getMonthValue(ledger.getMonth())
            );
            if (!rows.isEmpty()) {
                String normalizedMonth = normalizeMonth(ledger.getMonth());
                return composeLedgerForBuilding(
                        ledger.getBuildingId(),
                        ledger.getYear(),
                        normalizedMonth,
                        ledger.getId(),
                        rows
                );
            }
        }

        List<LedgerItemResponse> items = ledger.getItems()
                .stream()
                .map(i -> new LedgerItemResponse(i.getId(), i.getName(), i.getAmount(), i.getSectionKey(), i.getSectionLabel()))
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
        ledgerResponse.setTotalExpenses(ledger.getTotalExpenses());
        return ledgerResponse;
    }

    private void enrichFromMaintenanceRows(LedgerResponse response, List<Maintenance> rows) {
        double totalAmount = MaintenanceLedgerAggregation.canonicalTotalCollected(rows).doubleValue();
        int totalFlats = Math.max(rows.size(), 1);
        response.setTotalAmount(totalAmount);
        if (response.getTotalExpenses() == 0) {
            response.setTotalExpenses(0);
        }
        response.setTotalFlats(totalFlats);
        double billedTotal = MaintenanceLedgerAggregation.sumAmounts(rows).doubleValue();
        response.setPerFlatAmount(totalFlats == 0 ? 0 : billedTotal / totalFlats);
        response.setTotalBudget(totalAmount + response.getOpeningBalance());
        response.setClosingBalance(response.getTotalBudget() - response.getTotalExpenses());
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
        double fixedMaintenance = MaintenanceLedgerAggregation.fixedMaintenanceCollectionTotal(rows);
        double water = MaintenanceLedgerAggregation.sumWater(rows).doubleValue();
        List<LedgerItemResponse> items = new ArrayList<>();
        if (fixedMaintenance > 0.005) {
            items.add(new LedgerItemResponse(null, "Fixed Maintenance", fixedMaintenance, "COLLECTION", "Collection Amount"));
        }
        if (water > 0.005) {
            items.add(new LedgerItemResponse(null, "Water Bill", water, "COLLECTION", "Collection Amount"));
        }

        double appliancesTotal = MaintenanceLedgerAggregation.sumAppliances(rows).doubleValue();
        if (appliancesTotal > 0.005) {
            items.add(new LedgerItemResponse(null, "Appliances", appliancesTotal, "COLLECTION", "Collection Amount"));
        }
        double assetTotal = MaintenanceLedgerAggregation.buildingAssetAmount(rows).doubleValue();
        if (assetTotal > 0.005) {
            String desc = rows.stream()
                    .map(Maintenance::getAssetDescription)
                    .filter(s -> s != null && !s.isBlank())
                    .findFirst()
                    .orElse("");
            String label = desc.isBlank() ? "Asset" : "Asset - " + desc.trim();
            items.add(new LedgerItemResponse(
                    null,
                    label,
                    assetTotal,
                    "COLLECTION",
                    "Collection Amount"));
        }
        for (Map.Entry<String, BigDecimal> e : MaintenanceLedgerAggregation.mergeCustomExpenseTotals(rows).entrySet()) {
            double custom = e.getValue().doubleValue();
            if (custom > 0.005) {
                items.add(new LedgerItemResponse(
                        null,
                        e.getKey(),
                        custom,
                        "COLLECTION",
                        "Collection Amount"));
            }
        }
        return items;
    }

    private void applyNotebookSnapshot(LedgerResponse response, NotebookMonthly notebook) {
        response.setOpeningBalance(Boolean.TRUE.equals(notebook.getUsePreviousBalance())
                ? notebook.getOpeningBalance().doubleValue()
                : 0);
        response.setTotalExpenses(notebook.getTotalExpenses().doubleValue());
    }

    private List<LedgerItemResponse> buildItemsFromNotebook(NotebookMonthly notebook) {
        List<LedgerItemResponse> rows = new ArrayList<>();
        if (notebook.getExpenseBreakdown() != null) {
            notebook.getExpenseBreakdown().forEach((name, amount) -> {
                if (name == null || name.trim().isEmpty() || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                    return;
                }
                rows.add(new LedgerItemResponse(
                        null,
                        name.trim(),
                        amount.doubleValue(),
                        "EXPENSE",
                        "Expense Breakdown"
                ));
            });
        }
        return rows;
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
