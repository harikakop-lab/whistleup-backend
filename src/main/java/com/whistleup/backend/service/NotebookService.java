package com.whistleup.backend.service;

import com.whistleup.backend.entity.Ledger;
import com.whistleup.backend.entity.LedgerItem;
import com.whistleup.backend.entity.Maintenance;
import com.whistleup.backend.entity.NotebookMonthly;
import com.whistleup.backend.repository.LedgerRepository;
import com.whistleup.backend.repository.MaintenanceRepository;
import com.whistleup.backend.repository.NotebookMonthlyRepository;
import com.whistleup.backend.resource.NotebookBalanceResponse;
import com.whistleup.backend.resource.NotebookResponse;
import com.whistleup.backend.resource.NotebookUpsertRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NotebookService {

    private static final String SECTION_COLLECTION = "COLLECTION";
    private static final String SECTION_EXPENSE = "EXPENSE";
    private static final String SECTION_COLLECTION_LABEL = "Collection Amount";
    private static final String SECTION_EXPENSE_LABEL = "Expense Breakdown";

    private final NotebookMonthlyRepository notebookMonthlyRepository;
    private final LedgerRepository ledgerRepository;
    private final MaintenanceRepository maintenanceRepository;

    public NotebookResponse upsertNotebook(NotebookUpsertRequest request) {
        String normalizedMonth = normalizeMonth(request.getMonth());
        List<Maintenance> maintenanceRows = maintenanceRepository.findByBuildingIdAndMaintenanceYearAndMaintenanceMonth(
                request.getBuildingId(),
                request.getYear(),
                monthToNumber(normalizedMonth)
        );

        NotebookMonthly notebook = notebookMonthlyRepository
                .findByBuildingIdAndYearAndMonth(request.getBuildingId(), request.getYear(), normalizedMonth)
                .orElseGet(NotebookMonthly::new);

        notebook.setBuildingId(request.getBuildingId());
        notebook.setYear(request.getYear());
        notebook.setMonth(normalizedMonth);
        notebook.setUsePreviousBalance(Boolean.TRUE.equals(request.getUsePreviousBalance()));
        notebook.setOpeningBalance(safe(request.getOpeningBalance()));
        notebook.setExpenseBreakdown(normalizeBreakdown(request.getExpenseBreakdown()));

        if (!maintenanceRows.isEmpty()) {
            overlayMaintenanceOntoNotebookEntity(notebook, maintenanceRows);
        } else {
            notebook.setFixedMaintenance(safe(request.getFixedMaintenance()));
            notebook.setResidentCount(Math.max(0, request.getResidentCount() == null ? 0 : request.getResidentCount()));
            notebook.setWaterBillAmount(safe(request.getWaterBillAmount()));
            if (notebook.getFixedMaintenance().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Fixed maintenance is required when no maintenance rows exist for this month");
            }
        }

        recomputeDerivedTotals(notebook, maintenanceRows);

        NotebookMonthly saved = notebookMonthlyRepository.save(notebook);
        syncLedgerForNotebook(saved);
        return toResponse(saved, !maintenanceRows.isEmpty());
    }

    public Optional<NotebookResponse> tryGetNotebookForPeriod(String buildingId, Integer year, String month) {
        String normalizedMonth = normalizeMonth(month);
        List<Maintenance> rows = maintenanceRepository.findByBuildingIdAndMaintenanceYearAndMaintenanceMonth(
                buildingId,
                year,
                monthToNumber(normalizedMonth)
        );
        Optional<NotebookMonthly> persisted = notebookMonthlyRepository.findByBuildingIdAndYearAndMonth(
                buildingId,
                year,
                normalizedMonth
        );
        if (persisted.isEmpty() && rows.isEmpty()) {
            return Optional.empty();
        }
        NotebookMonthly view = scratchFrom(persisted.orElseGet(() -> emptyDraft(buildingId, year, normalizedMonth)));
        if (!rows.isEmpty()) {
            overlayMaintenanceOntoNotebookEntity(view, rows);
        }
        recomputeDerivedTotals(view, rows);
        return Optional.of(toResponse(view, !rows.isEmpty()));
    }

    public NotebookResponse getNotebook(String buildingId, Integer year, String month) {
        return tryGetNotebookForPeriod(buildingId, year, month)
                .orElseThrow(() -> new IllegalStateException("Notebook data not found for selected period"));
    }

    public Optional<NotebookMonthly> findNotebookEntity(String buildingId, Integer year, String month) {
        return notebookMonthlyRepository.findByBuildingIdAndYearAndMonth(buildingId, year, normalizeMonth(month));
    }

    /**
     * Re-syncs stored notebook totals and ledger items from maintenance when maintenance is saved again
     * for the same month (e.g. water added after initial base billing).
     */
    @Transactional
    public void refreshNotebookFromMaintenance(String buildingId, int year, int maintenanceMonth) {
        if (buildingId == null || buildingId.isBlank() || maintenanceMonth < 1 || maintenanceMonth > 12) {
            return;
        }
        String monthLabel = java.time.Month.of(maintenanceMonth).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        List<Maintenance> rows = maintenanceRepository.findByBuildingIdAndMaintenanceYearAndMaintenanceMonth(
                buildingId,
                year,
                maintenanceMonth);
        if (rows.isEmpty()) {
            return;
        }
        notebookMonthlyRepository.findByBuildingIdAndYearAndMonth(buildingId, year, monthLabel).ifPresent(notebook -> {
            overlayMaintenanceOntoNotebookEntity(notebook, rows);
            recomputeDerivedTotals(notebook, rows);
            NotebookMonthly saved = notebookMonthlyRepository.save(notebook);
            syncLedgerForNotebook(saved);
        });
    }

    public NotebookBalanceResponse getPreviousMonthBalance(String buildingId, Integer year, String month) {
        String normalizedMonth = normalizeMonth(month);
        YearMonth current = YearMonth.of(year, monthToNumber(normalizedMonth));
        YearMonth previous = current.minusMonths(1);
        String previousMonth = previous.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        BigDecimal previousBalance = notebookMonthlyRepository
                .findByBuildingIdAndYearAndMonth(buildingId, previous.getYear(), previousMonth)
                .map(NotebookMonthly::getClosingBalance)
                .orElse(BigDecimal.ZERO);

        return NotebookBalanceResponse.builder()
                .buildingId(buildingId)
                .year(previous.getYear())
                .month(previousMonth)
                .previousClosingBalance(scale(previousBalance))
                .build();
    }

    private void syncLedgerForNotebook(NotebookMonthly notebook) {
        Ledger ledger = ledgerRepository.findByYearAndMonthAndBuildingId(
                notebook.getYear(),
                notebook.getMonth(),
                notebook.getBuildingId()
        ).orElseGet(Ledger::new);

        ledger.setYear(notebook.getYear());
        ledger.setMonth(notebook.getMonth());
        ledger.setBuildingId(notebook.getBuildingId());
        ledger.setTotalExpenses(scale(notebook.getTotalExpenses()).doubleValue());

        List<Maintenance> maintRows = maintenanceRepository.findByBuildingIdAndMaintenanceYearAndMaintenanceMonth(
                notebook.getBuildingId(),
                notebook.getYear(),
                monthToNumber(notebook.getMonth())
        );

        ledger.getItems().clear();
        if (!maintRows.isEmpty()) {
            ledger.setTotalFlats(Math.max(1, maintRows.size()));
            ledger.setTotalAmount(scale(MaintenanceLedgerAggregation.sumAmounts(maintRows)).doubleValue());
            double perFlat = maintRows.isEmpty() ? 0 : ledger.getTotalAmount() / maintRows.size();
            ledger.setPerFlatAmount(perFlat);

            double fixed = MaintenanceLedgerAggregation.fixedMaintenanceCollectionTotal(maintRows);
            double water = MaintenanceLedgerAggregation.sumWater(maintRows).doubleValue();
            double appl = MaintenanceLedgerAggregation.sumAppliances(maintRows).doubleValue();
            if (fixed > 0.005) {
                ledger.getItems().add(LedgerItem.builder()
                        .name("Fixed Maintenance")
                        .amount(fixed)
                        .sectionKey(SECTION_COLLECTION)
                        .sectionLabel(SECTION_COLLECTION_LABEL)
                        .ledger(ledger)
                        .build());
            }
            if (water > 0.005) {
                ledger.getItems().add(LedgerItem.builder()
                        .name("Water Bill")
                        .amount(water)
                        .sectionKey(SECTION_COLLECTION)
                        .sectionLabel(SECTION_COLLECTION_LABEL)
                        .ledger(ledger)
                        .build());
            }
            if (appl > 0.005) {
                ledger.getItems().add(LedgerItem.builder()
                        .name("Appliances")
                        .amount(appl)
                        .sectionKey(SECTION_COLLECTION)
                        .sectionLabel(SECTION_COLLECTION_LABEL)
                        .ledger(ledger)
                        .build());
            }
        } else {
            ledger.setTotalFlats(Math.max(1, notebook.getResidentCount()));
            ledger.setTotalAmount(scale(notebook.getCollectionAmount()).doubleValue());
            ledger.setPerFlatAmount(notebook.getResidentCount() <= 0
                    ? 0
                    : scale(notebook.getFixedMaintenance()).doubleValue());
            ledger.getItems().add(LedgerItem.builder()
                    .name("Fixed Maintenance")
                    .amount(scale(notebook.getCollectionAmount()).doubleValue())
                    .sectionKey(SECTION_COLLECTION)
                    .sectionLabel(SECTION_COLLECTION_LABEL)
                    .ledger(ledger)
                    .build());
            if (notebook.getWaterBillAmount() != null && notebook.getWaterBillAmount().compareTo(BigDecimal.ZERO) > 0) {
                ledger.getItems().add(LedgerItem.builder()
                        .name("Water Bill")
                        .amount(scale(notebook.getWaterBillAmount()).doubleValue())
                        .sectionKey(SECTION_COLLECTION)
                        .sectionLabel(SECTION_COLLECTION_LABEL)
                        .ledger(ledger)
                        .build());
            }
        }

        notebook.getExpenseBreakdown().forEach((name, amount) -> {
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                return;
            }
            ledger.getItems().add(LedgerItem.builder()
                    .name(name)
                    .amount(scale(amount).doubleValue())
                    .sectionKey(SECTION_EXPENSE)
                    .sectionLabel(SECTION_EXPENSE_LABEL)
                    .ledger(ledger)
                    .build());
        });

        ledgerRepository.save(ledger);
    }

    private NotebookMonthly emptyDraft(String buildingId, Integer year, String monthName) {
        NotebookMonthly n = new NotebookMonthly();
        n.setBuildingId(buildingId);
        n.setYear(year);
        n.setMonth(monthName);
        n.setFixedMaintenance(BigDecimal.ZERO);
        n.setResidentCount(0);
        n.setWaterBillAmount(BigDecimal.ZERO);
        n.setUsePreviousBalance(false);
        n.setOpeningBalance(BigDecimal.ZERO);
        n.setExpenseBreakdown(new LinkedHashMap<>());
        return n;
    }

    private NotebookMonthly scratchFrom(NotebookMonthly e) {
        NotebookMonthly s = new NotebookMonthly();
        s.setId(e.getId());
        s.setBuildingId(e.getBuildingId());
        s.setYear(e.getYear());
        s.setMonth(e.getMonth());
        s.setFixedMaintenance(safe(e.getFixedMaintenance()));
        s.setResidentCount(e.getResidentCount() == null ? 0 : e.getResidentCount());
        s.setWaterBillAmount(safe(e.getWaterBillAmount()));
        s.setUsePreviousBalance(Boolean.TRUE.equals(e.getUsePreviousBalance()));
        s.setOpeningBalance(safe(e.getOpeningBalance()));
        s.setExpenseBreakdown(normalizeBreakdown(e.getExpenseBreakdown()));
        return s;
    }

    private void overlayMaintenanceOntoNotebookEntity(NotebookMonthly notebook, List<Maintenance> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        notebook.setResidentCount(rows.size());
        notebook.setWaterBillAmount(scale(MaintenanceLedgerAggregation.sumWater(rows)));
        notebook.setFixedMaintenance(scale(MaintenanceLedgerAggregation.resolveFixedPerFlat(rows, 2, RoundingMode.HALF_UP)));
    }

    private void recomputeDerivedTotals(NotebookMonthly notebook, List<Maintenance> rows) {
        BigDecimal collectionAmount;
        if (rows != null && !rows.isEmpty()) {
            collectionAmount = scale(MaintenanceLedgerAggregation.sumAmounts(rows));
        } else {
            collectionAmount = scale(notebook.getFixedMaintenance()
                    .multiply(BigDecimal.valueOf(Math.max(0, notebook.getResidentCount()))));
        }
        notebook.setCollectionAmount(collectionAmount);

        BigDecimal openingComponent = Boolean.TRUE.equals(notebook.getUsePreviousBalance())
                ? safe(notebook.getOpeningBalance())
                : BigDecimal.ZERO;
        BigDecimal totalBudget = collectionAmount.add(openingComponent);
        BigDecimal totalExpenses = notebook.getExpenseBreakdown().values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal closingBalance = totalBudget.subtract(totalExpenses);

        notebook.setTotalBudget(scale(totalBudget));
        notebook.setTotalExpenses(scale(totalExpenses));
        notebook.setClosingBalance(scale(closingBalance));
    }

    private NotebookResponse toResponse(NotebookMonthly notebook, boolean maintenanceAnchored) {
        return NotebookResponse.builder()
                .id(notebook.getId())
                .buildingId(notebook.getBuildingId())
                .year(notebook.getYear())
                .month(notebook.getMonth())
                .fixedMaintenance(scale(notebook.getFixedMaintenance()))
                .residentCount(notebook.getResidentCount())
                .waterBillAmount(scale(notebook.getWaterBillAmount()))
                .usePreviousBalance(Boolean.TRUE.equals(notebook.getUsePreviousBalance()))
                .openingBalance(scale(notebook.getOpeningBalance()))
                .collectionAmount(scale(notebook.getCollectionAmount()))
                .totalBudget(scale(notebook.getTotalBudget()))
                .totalExpenses(scale(notebook.getTotalExpenses()))
                .closingBalance(scale(notebook.getClosingBalance()))
                .expenseBreakdown(normalizeBreakdown(notebook.getExpenseBreakdown()))
                .maintenanceAnchored(maintenanceAnchored)
                .build();
    }

    private BigDecimal safe(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private BigDecimal scale(BigDecimal amount) {
        return safe(amount).setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, BigDecimal> normalizeBreakdown(Map<String, BigDecimal> raw) {
        Map<String, BigDecimal> normalized = new LinkedHashMap<>();
        if (raw == null || raw.isEmpty()) {
            return normalized;
        }
        for (Map.Entry<String, BigDecimal> entry : raw.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey().trim();
            if (key.isEmpty()) continue;
            BigDecimal amount = safe(entry.getValue());
            if (amount.compareTo(BigDecimal.ZERO) < 0) continue;
            normalized.put(key, scale(amount));
        }
        return normalized;
    }

    private String normalizeMonth(String month) {
        if (month == null || month.trim().isEmpty()) {
            throw new IllegalArgumentException("month is required");
        }
        String value = month.trim();
        String upper = value.toUpperCase(Locale.ENGLISH);
        try {
            return java.time.Month.valueOf(upper).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
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

    private int monthToNumber(String month) {
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
            default -> throw new IllegalStateException("Unexpected month " + month);
        };
    }
}
