package com.whistleup.backend.service;

import com.whistleup.backend.constants.IssueType;
import com.whistleup.backend.constants.MaintenanceStatus;
import com.whistleup.backend.entity.Maintenance;
import com.whistleup.backend.entity.Profile;
import com.whistleup.backend.exception.NotFoundException;
import com.whistleup.backend.repository.MaintenanceRepository;
import com.whistleup.backend.repository.ProfileRepository;
import com.whistleup.backend.resource.MaintenanceFlatChargeResource;
import com.whistleup.backend.resource.MaintenanceCreateResource;
import com.whistleup.backend.resource.MaintenanceMeterRowResource;
import com.whistleup.backend.resource.MaintenanceResponseResource;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private final MaintenanceRepository repository;
    private final InvoiceService invoiceService;
    private final ProfileRepository profileRepository;
    private final NotificationSendService notificationSendService;

    public List<MaintenanceResponseResource> createOrUpdateMaintenance(MaintenanceCreateResource maintenanceCreateResource) {
        return upsertMaintenanceRows(maintenanceCreateResource, false);
    }

    public List<MaintenanceResponseResource> updateMaintenance(MaintenanceCreateResource maintenanceCreateResource) {
        return upsertMaintenanceRows(maintenanceCreateResource, true);
    }

    private List<MaintenanceResponseResource> upsertMaintenanceRows(
            MaintenanceCreateResource maintenanceCreateResource,
            boolean additiveUpdate) {
        List<Profile> residentsInTheBuilding = profileRepository.findByBuildingId(maintenanceCreateResource.getBuildingId());
        if (CollectionUtils.isEmpty(residentsInTheBuilding)) {
            return List.of();
        }

        Map<String, BigDecimal> explicitAmountByFlat = toExplicitAmountByFlat(maintenanceCreateResource.getFlatCharges());
        BigDecimal sharedExpenseTotal = resolveSharedExpenseTotal(maintenanceCreateResource);
        BigDecimal defaultWaterPerFlat = resolveDefaultWaterPerFlat(maintenanceCreateResource, residentsInTheBuilding.size());
        Map<String, BigDecimal> waterByFlat = resolveWaterByFlat(maintenanceCreateResource, residentsInTheBuilding, defaultWaterPerFlat);
        BigDecimal basePerFlat = residentsInTheBuilding.isEmpty()
                ? BigDecimal.ZERO
                : sharedExpenseTotal.divide(BigDecimal.valueOf(residentsInTheBuilding.size()), 2, RoundingMode.HALF_UP);
        BigDecimal watchmanPerFlat = splitPerFlat(maintenanceCreateResource.getWatchmanSalary(), residentsInTheBuilding.size());
        BigDecimal garbagePerFlat = splitPerFlat(maintenanceCreateResource.getGarbageCollection(), residentsInTheBuilding.size());
        BigDecimal liftPerFlat = splitPerFlat(maintenanceCreateResource.getLiftMaintenance(), residentsInTheBuilding.size());
        BigDecimal electricityPerFlat = splitPerFlat(maintenanceCreateResource.getElectricityCommon(), residentsInTheBuilding.size());
        BigDecimal motorPerFlat = splitPerFlat(maintenanceCreateResource.getMotorPump(), residentsInTheBuilding.size());
        BigDecimal miscPerFlat = splitPerFlat(maintenanceCreateResource.getMiscellaneous(), residentsInTheBuilding.size());

        List<Maintenance> savedRows = residentsInTheBuilding.stream()
                .map(profile -> upsertMaintenanceRow(
                        maintenanceCreateResource,
                        profile,
                        explicitAmountByFlat,
                        basePerFlat,
                        waterByFlat,
                        defaultWaterPerFlat,
                        additiveUpdate,
                        watchmanPerFlat,
                        garbagePerFlat,
                        liftPerFlat,
                        electricityPerFlat,
                        motorPerFlat,
                        miscPerFlat
                ))
                .toList();

        notifyResidents(savedRows);
        return savedRows.stream().map(this::toResponse).toList();
    }

    private Maintenance upsertMaintenanceRow(
            MaintenanceCreateResource req,
            Profile profile,
            Map<String, BigDecimal> explicitAmountByFlat,
            BigDecimal basePerFlat,
            Map<String, BigDecimal> waterByFlat,
            BigDecimal defaultWaterPerFlat,
            boolean additiveUpdate,
            BigDecimal watchmanPerFlat,
            BigDecimal garbagePerFlat,
            BigDecimal liftPerFlat,
            BigDecimal electricityPerFlat,
            BigDecimal motorPerFlat,
            BigDecimal miscPerFlat) {
        Maintenance maintenance;
        String phone = profile.getPhone();
        String flatNo = normalizeFlatNo(profile.getFlatNo());

        BigDecimal resolvedAmount = explicitAmountByFlat.getOrDefault(flatNo, null);
        if (resolvedAmount == null) {
            BigDecimal water = waterByFlat.getOrDefault(flatNo, defaultWaterPerFlat);
            resolvedAmount = basePerFlat.add(Objects.requireNonNullElse(water, BigDecimal.ZERO));
        }

        if (resolvedAmount.compareTo(BigDecimal.ZERO) <= 0 && req.getAmount() != null) {
            resolvedAmount = req.getAmount();
        }

        BigDecimal finalResolvedAmount = resolvedAmount;
        maintenance = repository.findByProfileIdAndBuildingIdAndMaintenanceYearAndMaintenanceMonth(
                        phone,
                        req.getBuildingId(),
                        req.getYear(),
                        req.getMonth()
                )
                .orElseGet(() -> Maintenance.builder()
                        .profileId(phone)
                        .maintenanceYear(req.getYear())
                        .maintenanceMonth(req.getMonth())
                        .amount(finalResolvedAmount)
                        .dueDate(YearMonth.now().atEndOfMonth())
                        .status(MaintenanceStatus.PENDING)
                        .buildingId(req.getBuildingId())
                        .build());
        if (additiveUpdate && maintenance.getId() != null) {
            maintenance.setAmount(addNullable(maintenance.getAmount(), resolvedAmount));
        } else {
            maintenance.setAmount(resolvedAmount);
        }
        if (Objects.nonNull(req.getDueDate())) {
            maintenance.setDueDate(req.getDueDate());
        } else if (maintenance.getDueDate() == null) {
            maintenance.setDueDate(YearMonth.now().atEndOfMonth());
        }
        maintenance.setWaterMode(normalizeMode(req.getWaterMode()));
        if (additiveUpdate && maintenance.getId() != null) {
            maintenance.setWatchmanSalary(addNullable(maintenance.getWatchmanSalary(), watchmanPerFlat));
            maintenance.setGarbageCollection(addNullable(maintenance.getGarbageCollection(), garbagePerFlat));
            maintenance.setLiftMaintenance(addNullable(maintenance.getLiftMaintenance(), liftPerFlat));
            maintenance.setElectricityCommon(addNullable(maintenance.getElectricityCommon(), electricityPerFlat));
            maintenance.setMotorPump(addNullable(maintenance.getMotorPump(), motorPerFlat));
            maintenance.setMiscellaneous(addNullable(maintenance.getMiscellaneous(), miscPerFlat));
            maintenance.setWaterAmount(addNullable(
                    maintenance.getWaterAmount(),
                    waterByFlat.getOrDefault(flatNo, defaultWaterPerFlat)
            ));
        } else {
            maintenance.setWatchmanSalary(watchmanPerFlat);
            maintenance.setGarbageCollection(garbagePerFlat);
            maintenance.setLiftMaintenance(liftPerFlat);
            maintenance.setElectricityCommon(electricityPerFlat);
            maintenance.setMotorPump(motorPerFlat);
            maintenance.setMiscellaneous(miscPerFlat);
            maintenance.setWaterAmount(waterByFlat.getOrDefault(flatNo, defaultWaterPerFlat));
        }
        return repository.save(maintenance);
    }

    private BigDecimal splitPerFlat(BigDecimal value, int totalProfiles) {
        if (value == null || totalProfiles <= 0) {
            return BigDecimal.ZERO;
        }
        return value.divide(BigDecimal.valueOf(totalProfiles), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal addNullable(BigDecimal existing, BigDecimal incoming) {
        return Objects.requireNonNullElse(existing, BigDecimal.ZERO)
                .add(Objects.requireNonNullElse(incoming, BigDecimal.ZERO));
    }

    private void notifyResidents(List<Maintenance> rows) {
        for (Maintenance row : rows) {
            try {
                notificationSendService.notifyUser(
                        Long.valueOf(row.getProfileId()),
                        "Maintenance",
                        "Please pay your maintenance amount of " + row.getAmount() + " rupees for this month.",
                        IssueType.ALERT.name()
                );
            } catch (Exception ignore) {
                // Keep maintenance generation resilient even if push notification fails.
            }
        }
    }

    private Map<String, BigDecimal> toExplicitAmountByFlat(List<MaintenanceFlatChargeResource> flatCharges) {
        Map<String, BigDecimal> amountByFlat = new HashMap<>();
        if (CollectionUtils.isEmpty(flatCharges)) {
            return amountByFlat;
        }
        for (MaintenanceFlatChargeResource item : flatCharges) {
            String flat = normalizeFlatNo(item.getFlatNumber());
            if (flat == null) {
                continue;
            }
            BigDecimal amount = Objects.requireNonNullElse(item.getAmount(), BigDecimal.ZERO);
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                amountByFlat.put(flat, amount);
            }
        }
        return amountByFlat;
    }

    private BigDecimal resolveSharedExpenseTotal(MaintenanceCreateResource req) {
        BigDecimal total = BigDecimal.ZERO;
        total = total.add(Objects.requireNonNullElse(req.getWatchmanSalary(), BigDecimal.ZERO));
        total = total.add(Objects.requireNonNullElse(req.getGarbageCollection(), BigDecimal.ZERO));
        total = total.add(Objects.requireNonNullElse(req.getLiftMaintenance(), BigDecimal.ZERO));
        total = total.add(Objects.requireNonNullElse(req.getElectricityCommon(), BigDecimal.ZERO));
        total = total.add(Objects.requireNonNullElse(req.getMotorPump(), BigDecimal.ZERO));
        total = total.add(Objects.requireNonNullElse(req.getMiscellaneous(), BigDecimal.ZERO));
        if (total.compareTo(BigDecimal.ZERO) == 0 && req.getAmount() != null) {
            return req.getAmount().multiply(BigDecimal.valueOf(Objects.requireNonNullElse(req.getTotalFlats(), 1)));
        }
        return total;
    }

    private BigDecimal resolveDefaultWaterPerFlat(MaintenanceCreateResource req, int profileCount) {
        int flats = resolveTotalFlats(req, profileCount);
        if (flats <= 0) {
            return BigDecimal.ZERO;
        }
        String mode = normalizeMode(req.getWaterMode());
        BigDecimal totalWater;
        if ("FIXED".equals(mode)) {
            totalWater = Objects.requireNonNullElse(req.getFixedWaterBill(), BigDecimal.ZERO);
            return totalWater.divide(BigDecimal.valueOf(flats), 2, RoundingMode.HALF_UP);
        }
        if ("MASTER".equals(mode)) {
            totalWater = Objects.requireNonNullElse(req.getMasterWaterBill(), BigDecimal.ZERO);
            return totalWater.divide(BigDecimal.valueOf(flats), 2, RoundingMode.HALF_UP);
        }
        if ("MIXED".equals(mode)) {
            BigDecimal pool = Objects.requireNonNullElse(req.getMixedFixedPool(), BigDecimal.ZERO);
            return pool.divide(BigDecimal.valueOf(flats), 2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private Map<String, BigDecimal> resolveWaterByFlat(
            MaintenanceCreateResource req,
            List<Profile> residents,
            BigDecimal defaultWaterPerFlat) {
        Map<String, BigDecimal> waterByFlat = new HashMap<>();
        String mode = normalizeMode(req.getWaterMode());
        if ("INDIVIDUAL".equals(mode)) {
            BigDecimal rate = Objects.requireNonNullElse(req.getIndividualRatePerUnit(), BigDecimal.ZERO);
            List<MaintenanceMeterRowResource> rows = req.getIndividualRows() == null ? List.of() : req.getIndividualRows();
            for (MaintenanceMeterRowResource row : rows) {
                if (row == null || row.getFlatNumber() == null) continue;
                BigDecimal units = Objects.requireNonNullElse(row.getUnits(), BigDecimal.ZERO);
                waterByFlat.put(normalizeFlatNo(row.getFlatNumber()), units.multiply(rate));
            }
            return waterByFlat;
        }
        if ("MIXED".equals(mode)) {
            BigDecimal mixedRate = Objects.requireNonNullElse(req.getMixedRatePerUnit(), BigDecimal.ZERO);
            List<MaintenanceMeterRowResource> mixedRows = req.getMixedMeterRows() == null ? List.of() : req.getMixedMeterRows();
            for (MaintenanceMeterRowResource row : mixedRows) {
                if (row == null || row.getFlatNumber() == null) continue;
                BigDecimal units = Objects.requireNonNullElse(row.getUnits(), BigDecimal.ZERO);
                waterByFlat.put(normalizeFlatNo(row.getFlatNumber()), units.multiply(mixedRate));
            }

            List<String> allFlats = req.getAllFlats();
            if (CollectionUtils.isEmpty(allFlats)) {
                allFlats = residents.stream()
                        .map(Profile::getFlatNo)
                        .filter(Objects::nonNull)
                        .map(this::normalizeFlatNo)
                        .toList();
            } else {
                allFlats = allFlats.stream().filter(Objects::nonNull).map(this::normalizeFlatNo).toList();
            }
            long nonMeteredCount = allFlats.stream().filter(flat -> !waterByFlat.containsKey(flat)).count();
            BigDecimal nonMeteredShare = nonMeteredCount <= 0
                    ? BigDecimal.ZERO
                    : Objects.requireNonNullElse(req.getMixedFixedPool(), BigDecimal.ZERO)
                    .divide(BigDecimal.valueOf(nonMeteredCount), 2, RoundingMode.HALF_UP);
            for (String flat : allFlats) {
                waterByFlat.putIfAbsent(flat, nonMeteredShare);
            }
            return waterByFlat;
        }
        for (Profile profile : residents) {
            String flat = normalizeFlatNo(profile.getFlatNo());
            if (flat != null) {
                waterByFlat.put(flat, defaultWaterPerFlat);
            }
        }
        return waterByFlat;
    }

    private int resolveTotalFlats(MaintenanceCreateResource req, int profileCount) {
        if (req.getTotalFlats() != null && req.getTotalFlats() > 0) {
            return req.getTotalFlats();
        }
        if (!CollectionUtils.isEmpty(req.getAllFlats())) {
            return req.getAllFlats().size();
        }
        return profileCount;
    }

    private String normalizeMode(String mode) {
        return mode == null ? "" : mode.trim().toUpperCase(Locale.ENGLISH);
    }

    private String normalizeFlatNo(String flat) {
        return flat == null ? null : flat.trim().toUpperCase(Locale.ENGLISH);
    }

    public List<MaintenanceResponseResource> getByBuilding(String buildingId) {
        return repository.findByBuildingIdOrderByMaintenanceYearDescMaintenanceMonthDesc(buildingId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<MaintenanceResponseResource> getByBuildingAndPeriod(String buildingId, Integer year, Integer month) {
        return repository.findByBuildingIdAndMaintenanceYearAndMaintenanceMonth(buildingId, year, month)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public void markAsPaid(Long maintenanceId) {

        Maintenance m = repository.findById(maintenanceId)
                .orElseThrow(() -> new NotFoundException("Maintenance not found"));

        if (m.getStatus() == MaintenanceStatus.PAID) return;

        m.setStatus(MaintenanceStatus.PAID);
        m.setPaidDate(LocalDate.now());

        String invoicePath = invoiceService.generateInvoice(m);
        m.setInvoicePath(invoicePath);

        repository.save(m);
    }

    private MaintenanceResponseResource toResponse(Maintenance m) {
        return MaintenanceResponseResource.builder()
                .id(m.getId())
                .profileId(m.getProfileId())
                .buildingId(m.getBuildingId())
                .year(m.getMaintenanceYear())
                .month(m.getMaintenanceMonth())
                .monthLabel(
                    Month.of(m.getMaintenanceMonth()).name() + " Maintenance"
                )
                .amount(m.getAmount())
                .watchmanSalary(m.getWatchmanSalary())
                .garbageCollection(m.getGarbageCollection())
                .liftMaintenance(m.getLiftMaintenance())
                .electricityCommon(m.getElectricityCommon())
                .motorPump(m.getMotorPump())
                .miscellaneous(m.getMiscellaneous())
                .waterAmount(m.getWaterAmount())
                .waterMode(m.getWaterMode())
                .dueDate(m.getDueDate())
                .status(m.getStatus())
                .paidDate(m.getPaidDate())
                .invoiceAvailable(m.getInvoicePath() != null)
                .build();
    }

    public Maintenance getEntity(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("No Maintenance found"));
    }

    public List<MaintenanceResponseResource> getMaintenanceByProfileId(String username) {
        return repository.findByProfileIdOrderByMaintenanceYearDescMaintenanceMonthDesc(username)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<Maintenance> getListOfPendingMaintenanceForCurrentMonth() {
        val year = LocalDate.now().getYear();
        val month = LocalDate.now().getMonthValue();
        return repository.findPendingMaintenanceByCurrentMonthAndYear(month, year);
    }
}
