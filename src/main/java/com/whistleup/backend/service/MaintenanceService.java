package com.whistleup.backend.service;

import com.whistleup.backend.constants.IssueType;
import com.whistleup.backend.constants.MaintenanceStatus;
import com.whistleup.backend.entity.BuildingDetails;
import com.whistleup.backend.entity.Maintenance;
import com.whistleup.backend.entity.Profile;
import com.whistleup.backend.exception.NotFoundException;
import com.whistleup.backend.repository.BuildingDetailsRepository;
import com.whistleup.backend.repository.MaintenanceRepository;
import com.whistleup.backend.repository.ProfileRepository;
import com.whistleup.backend.resource.MaintenanceFlatChargeResource;
import com.whistleup.backend.resource.MaintenanceCreateResource;
import com.whistleup.backend.resource.MaintenanceMeterRowResource;
import com.whistleup.backend.resource.MaintenanceAppliancesOptInResource;
import com.whistleup.backend.resource.MaintenanceResponseResource;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private final MaintenanceRepository repository;
    private final InvoiceService invoiceService;
    private final ProfileRepository profileRepository;
    private final BuildingDetailsRepository buildingDetailsRepository;
    private final NotificationSendService notificationSendService;
    private final FileStorageService fileStorageService;

    @Value("${app.base-url}")
    private String appBaseUrl;

    private static final long MAX_PAYMENT_PROOF_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_PAYMENT_METHODS =
            Set.of("UPI", "BANK_TRANSFER", "CASH", "CHEQUE");

    public List<MaintenanceResponseResource> createOrUpdateMaintenance(MaintenanceCreateResource maintenanceCreateResource) {
        return upsertMaintenanceRows(maintenanceCreateResource);
    }

    public List<MaintenanceResponseResource> updateMaintenance(MaintenanceCreateResource maintenanceCreateResource) {
        return upsertMaintenanceRows(maintenanceCreateResource);
    }

    private List<MaintenanceResponseResource> upsertMaintenanceRows(
            MaintenanceCreateResource maintenanceCreateResource) {
        List<Profile> residentsInTheBuilding = profileRepository.findByBuildingId(maintenanceCreateResource.getBuildingId());
        if (CollectionUtils.isEmpty(residentsInTheBuilding)) {
            return List.of();
        }

        Map<String, BigDecimal> explicitAmountByFlat = toExplicitAmountByFlat(maintenanceCreateResource.getFlatCharges());
        Map<String, BigDecimal> explicitAppliancesByFlat = toExplicitAppliancesByFlat(maintenanceCreateResource.getFlatCharges());
        Map<String, BigDecimal> explicitAppliancesByPhone = toExplicitAppliancesByPhone(
                maintenanceCreateResource.getApplianceFeesByPhone());
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
        Map<String, BigDecimal> customExpensesPerFlat = splitCustomExpensesPerFlat(
                maintenanceCreateResource.getCustomExpenses(),
                residentsInTheBuilding.size()
        );

        BuildingDetails buildingDetails = resolveBuilding(maintenanceCreateResource.getBuildingId());
        boolean applianceAware = buildingDetails != null && buildingDetails.isAppliancesNeeded();
        BigDecimal appliancePool = Objects.requireNonNullElse(maintenanceCreateResource.getAppliancesTotalAmount(), BigDecimal.ZERO);
        List<Profile> optedEligible = residentsInTheBuilding.stream()
                .filter(p -> Boolean.TRUE.equals(p.getAppliancesMaintenanceOptIn()))
                .filter(p -> p.getAppliancesJson() != null && !p.getAppliancesJson().isBlank())
                .toList();
        int optCount = optedEligible.size();
        BigDecimal perApplianceShare = (applianceAware && optCount > 0 && appliancePool.compareTo(BigDecimal.ZERO) > 0)
                ? appliancePool.divide(BigDecimal.valueOf(optCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        Set<String> optedPhones = new HashSet<>(optedEligible.stream().map(Profile::getPhone).toList());
        Map<String, Long> optedCountByFlat = optedEligible.stream()
                .map(p -> normalizeFlatNo(p.getFlatNo()))
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(f -> f, Collectors.counting()));

        List<Maintenance> savedRows = residentsInTheBuilding.stream()
                .map(profile -> upsertMaintenanceRow(
                        maintenanceCreateResource,
                        profile,
                        explicitAmountByFlat,
                        explicitAppliancesByFlat,
                        explicitAppliancesByPhone,
                        optedCountByFlat,
                        basePerFlat,
                        waterByFlat,
                        defaultWaterPerFlat,
                        watchmanPerFlat,
                        garbagePerFlat,
                        liftPerFlat,
                        electricityPerFlat,
                        motorPerFlat,
                        miscPerFlat,
                        customExpensesPerFlat,
                        optedPhones,
                        perApplianceShare
                ))
                .toList();

        notifyResidents(savedRows);
        return savedRows.stream().map(this::toResponse).toList();
    }

    private BuildingDetails resolveBuilding(String buildingId) {
        if (buildingId == null || buildingId.isBlank()) {
            return null;
        }
        try {
            return buildingDetailsRepository.findById(Long.valueOf(buildingId.trim())).orElse(null);
        } catch (Exception ex) {
            return null;
        }
    }

    public List<MaintenanceAppliancesOptInResource> listAppliancesOptInFlats(String buildingId) {
        List<Profile> residents = profileRepository.findByBuildingId(buildingId);
        if (CollectionUtils.isEmpty(residents)) {
            return List.of();
        }
        return residents.stream()
                .filter(p -> Boolean.TRUE.equals(p.getAppliancesMaintenanceOptIn()))
                .filter(p -> p.getAppliancesJson() != null && !p.getAppliancesJson().isBlank())
                .map(p -> MaintenanceAppliancesOptInResource.builder()
                        .phone(p.getPhone())
                        .name(p.getName())
                        .flatNo(p.getFlatNo())
                        .appliancesJson(p.getAppliancesJson())
                        .build())
                .toList();
    }

    private Maintenance upsertMaintenanceRow(
            MaintenanceCreateResource req,
            Profile profile,
            Map<String, BigDecimal> explicitAmountByFlat,
            Map<String, BigDecimal> explicitAppliancesByFlat,
            Map<String, BigDecimal> explicitAppliancesByPhone,
            Map<String, Long> optedCountByFlat,
            BigDecimal basePerFlat,
            Map<String, BigDecimal> waterByFlat,
            BigDecimal defaultWaterPerFlat,
            BigDecimal watchmanPerFlat,
            BigDecimal garbagePerFlat,
            BigDecimal liftPerFlat,
            BigDecimal electricityPerFlat,
            BigDecimal motorPerFlat,
            BigDecimal miscPerFlat,
            Map<String, BigDecimal> customExpensesPerFlat,
            Set<String> optedAppliancePhones,
            BigDecimal perApplianceShare) {
        Maintenance maintenance;
        String phone = profile.getPhone();
        String flatNo = normalizeFlatNo(profile.getFlatNo());

        BigDecimal resolvedAmount = explicitAmountByFlat.getOrDefault(flatNo, null);
        boolean hasExplicitFlatTotal = resolvedAmount != null;
        if (resolvedAmount == null) {
            BigDecimal water = waterByFlat.getOrDefault(flatNo, defaultWaterPerFlat);
            resolvedAmount = basePerFlat.add(Objects.requireNonNullElse(water, BigDecimal.ZERO));
        }

        if (resolvedAmount.compareTo(BigDecimal.ZERO) <= 0 && req.getAmount() != null) {
            resolvedAmount = req.getAmount();
        }

        BigDecimal applianceShare = resolveApplianceShare(
                phone,
                flatNo,
                explicitAppliancesByPhone,
                explicitAppliancesByFlat,
                optedAppliancePhones,
                optedCountByFlat,
                perApplianceShare);
        if (!hasExplicitFlatTotal) {
            resolvedAmount = resolvedAmount.add(applianceShare);
        }

        BigDecimal finalResolvedAmount = resolvedAmount;
        val maintenanceOptional = repository.findByProfileIdAndBuildingIdAndMaintenanceYearAndMaintenanceMonth(
                phone,
                req.getBuildingId(),
                req.getYear(),
                req.getMonth()
        );
        YearMonth billingPeriod = YearMonth.of(req.getYear(), req.getMonth());
        
        maintenance = maintenanceOptional
                .orElseGet(() -> Maintenance.builder()
                        .profileId(phone)
                        .maintenanceYear(req.getYear())
                        .maintenanceMonth(req.getMonth())
                        .amount(finalResolvedAmount)
                        .dueDate(billingPeriod.atEndOfMonth())
                        .status(MaintenanceStatus.PENDING)
                        .buildingId(req.getBuildingId())
                        .build());
        if (maintenanceOptional.isPresent() && maintenance.getId() != null) {
            maintenance.setAmount(addNullable(maintenance.getAmount(), resolvedAmount));
        } else {
            maintenance.setAmount(resolvedAmount);
        }
        if (Objects.nonNull(req.getDueDate())) {
            maintenance.setDueDate(req.getDueDate());
        } else if (maintenance.getDueDate() == null) {
            maintenance.setDueDate(billingPeriod.atEndOfMonth());
        }
        maintenance.setWaterMode(normalizeMode(req.getWaterMode()));
        if (maintenanceOptional.isPresent() && maintenance.getId() != null) {
            maintenance.setWatchmanSalary(addNullable(maintenance.getWatchmanSalary(), watchmanPerFlat));
            maintenance.setGarbageCollection(addNullable(maintenance.getGarbageCollection(), garbagePerFlat));
            maintenance.setLiftMaintenance(addNullable(maintenance.getLiftMaintenance(), liftPerFlat));
            maintenance.setElectricityCommon(addNullable(maintenance.getElectricityCommon(), electricityPerFlat));
            maintenance.setMotorPump(addNullable(maintenance.getMotorPump(), motorPerFlat));
            maintenance.setMiscellaneous(addNullable(maintenance.getMiscellaneous(), miscPerFlat));
            maintenance.setCustomExpenses(mergeCustomExpenseMaps(
                    maintenance.getCustomExpenses(),
                    customExpensesPerFlat
            ));
            maintenance.setWaterAmount(addNullable(
                    maintenance.getWaterAmount(),
                    waterByFlat.getOrDefault(flatNo, defaultWaterPerFlat)
            ));
            maintenance.setAppliancesAmount(addNullable(maintenance.getAppliancesAmount(), applianceShare));
        } else {
            maintenance.setWatchmanSalary(watchmanPerFlat);
            maintenance.setGarbageCollection(garbagePerFlat);
            maintenance.setLiftMaintenance(liftPerFlat);
            maintenance.setElectricityCommon(electricityPerFlat);
            maintenance.setMotorPump(motorPerFlat);
            maintenance.setMiscellaneous(miscPerFlat);
            maintenance.setCustomExpenses(new LinkedHashMap<>(customExpensesPerFlat));
            maintenance.setWaterAmount(waterByFlat.getOrDefault(flatNo, defaultWaterPerFlat));
            maintenance.setAppliancesAmount(applianceShare);
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

    /**
     * Per-flat appliance amounts from the client. Only flats with a non-null {@code appliancesAmount} on a charge row
     * participate; duplicate flat keys are merged by sum. When the map is empty for all rows, callers fall back to
     * equal split ({@code perApplianceShare}) in {@link #upsertMaintenanceRow}.
     */
    private Map<String, BigDecimal> toExplicitAppliancesByFlat(List<MaintenanceFlatChargeResource> flatCharges) {
        Map<String, BigDecimal> byFlat = new HashMap<>();
        if (CollectionUtils.isEmpty(flatCharges)) {
            return byFlat;
        }
        for (MaintenanceFlatChargeResource item : flatCharges) {
            if (item.getAppliancesAmount() == null) {
                continue;
            }
            String flat = normalizeFlatNo(item.getFlatNumber());
            if (flat == null) {
                continue;
            }
            byFlat.merge(flat, item.getAppliancesAmount(), BigDecimal::add);
        }
        return byFlat;
    }

    private Map<String, BigDecimal> toExplicitAppliancesByPhone(Map<String, BigDecimal> raw) {
        if (raw == null || raw.isEmpty()) {
            return Map.of();
        }
        Map<String, BigDecimal> byPhone = new HashMap<>();
        for (Map.Entry<String, BigDecimal> e : raw.entrySet()) {
            if (e.getKey() == null) {
                continue;
            }
            String ph = e.getKey().trim();
            if (ph.isEmpty()) {
                continue;
            }
            byPhone.put(ph, Objects.requireNonNullElse(e.getValue(), BigDecimal.ZERO));
        }
        return byPhone;
    }

    private BigDecimal resolveApplianceShare(
            String phone,
            String flatNo,
            Map<String, BigDecimal> explicitAppliancesByPhone,
            Map<String, BigDecimal> explicitAppliancesByFlat,
            Set<String> optedAppliancePhones,
            Map<String, Long> optedCountByFlat,
            BigDecimal perApplianceShare) {
        if (!explicitAppliancesByPhone.isEmpty()) {
            return explicitAppliancesByPhone.getOrDefault(phone, BigDecimal.ZERO);
        }
        if (explicitAppliancesByFlat.containsKey(flatNo)) {
            if (!optedAppliancePhones.contains(phone)) {
                return BigDecimal.ZERO;
            }
            BigDecimal pool = Objects.requireNonNullElse(explicitAppliancesByFlat.get(flatNo), BigDecimal.ZERO);
            long n = optedCountByFlat.getOrDefault(flatNo, 1L);
            return pool.divide(BigDecimal.valueOf(Math.max(1L, n)), 2, RoundingMode.HALF_UP);
        }
        if (optedAppliancePhones.contains(phone)) {
            return Objects.requireNonNullElse(perApplianceShare, BigDecimal.ZERO);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal resolveSharedExpenseTotal(MaintenanceCreateResource req) {
        BigDecimal total = BigDecimal.ZERO;
        total = total.add(Objects.requireNonNullElse(req.getWatchmanSalary(), BigDecimal.ZERO));
        total = total.add(Objects.requireNonNullElse(req.getGarbageCollection(), BigDecimal.ZERO));
        total = total.add(Objects.requireNonNullElse(req.getLiftMaintenance(), BigDecimal.ZERO));
        total = total.add(Objects.requireNonNullElse(req.getElectricityCommon(), BigDecimal.ZERO));
        total = total.add(Objects.requireNonNullElse(req.getMotorPump(), BigDecimal.ZERO));
        total = total.add(Objects.requireNonNullElse(req.getMiscellaneous(), BigDecimal.ZERO));
        total = total.add(normalizeCustomExpenses(req.getCustomExpenses()).values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        if (total.compareTo(BigDecimal.ZERO) == 0 && req.getAmount() != null) {
            return req.getAmount().multiply(BigDecimal.valueOf(Objects.requireNonNullElse(req.getTotalFlats(), 1)));
        }
        return total;
    }

    private Map<String, BigDecimal> splitCustomExpensesPerFlat(
            Map<String, BigDecimal> customExpenses,
            int totalProfiles) {
        Map<String, BigDecimal> normalized = normalizeCustomExpenses(customExpenses);
        if (normalized.isEmpty() || totalProfiles <= 0) {
            return new LinkedHashMap<>();
        }
        Map<String, BigDecimal> perFlat = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> entry : normalized.entrySet()) {
            perFlat.put(
                    entry.getKey(),
                    entry.getValue().divide(BigDecimal.valueOf(totalProfiles), 2, RoundingMode.HALF_UP)
            );
        }
        return perFlat;
    }

    private Map<String, BigDecimal> normalizeCustomExpenses(Map<String, BigDecimal> customExpenses) {
        Map<String, BigDecimal> normalized = new LinkedHashMap<>();
        if (customExpenses == null || customExpenses.isEmpty()) {
            return normalized;
        }
        for (Map.Entry<String, BigDecimal> entry : customExpenses.entrySet()) {
            String rawKey = entry.getKey();
            String key = rawKey == null ? "" : rawKey.trim();
            if (key.isEmpty()) {
                continue;
            }
            BigDecimal amount = Objects.requireNonNullElse(entry.getValue(), BigDecimal.ZERO);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            normalized.merge(key, amount, BigDecimal::add);
        }
        return normalized;
    }

    private Map<String, BigDecimal> mergeCustomExpenseMaps(
            Map<String, BigDecimal> existing,
            Map<String, BigDecimal> incoming) {
        Map<String, BigDecimal> merged = new LinkedHashMap<>();
        if (existing != null) {
            merged.putAll(normalizeCustomExpenses(existing));
        }
        Map<String, BigDecimal> normalizedIncoming = normalizeCustomExpenses(incoming);
        for (Map.Entry<String, BigDecimal> entry : normalizedIncoming.entrySet()) {
            merged.merge(entry.getKey(), entry.getValue(), BigDecimal::add);
        }
        return merged;
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

    public void markAsPaid(
            Long maintenanceId,
            String paymentMethod,
            String transactionReference,
            MultipartFile paymentProof) {

        Maintenance m = repository.findById(maintenanceId)
                .orElseThrow(() -> new NotFoundException("Maintenance not found"));

        String normalizedPaymentMethod = normalizePaymentMethod(paymentMethod);
        String normalizedReference = normalizeReference(transactionReference);
        validatePaymentProof(paymentProof);

        if (m.getStatus() == MaintenanceStatus.PAID) {
            m.setPaymentMethod(normalizedPaymentMethod);
            m.setPaymentReference(normalizedReference);
            if (paymentProof != null && !paymentProof.isEmpty()) {
                String proofFileName = fileStorageService.saveMaintenancePaymentProof(maintenanceId, paymentProof);
                m.setPaymentProofFileName(proofFileName);
            }
            repository.save(m);
            return;
        }

        m.setStatus(MaintenanceStatus.PAID);
        m.setPaidDate(LocalDate.now());
        m.setPaymentMethod(normalizedPaymentMethod);
        m.setPaymentReference(normalizedReference);
        if (paymentProof != null && !paymentProof.isEmpty()) {
            String proofFileName = fileStorageService.saveMaintenancePaymentProof(maintenanceId, paymentProof);
            m.setPaymentProofFileName(proofFileName);
        }

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
                .customExpenses(m.getCustomExpenses())
                .waterAmount(m.getWaterAmount())
                .appliancesAmount(m.getAppliancesAmount())
                .waterMode(m.getWaterMode())
                .dueDate(m.getDueDate())
                .status(m.getStatus())
                .paidDate(m.getPaidDate())
                .paymentMethod(m.getPaymentMethod())
                .paymentReference(m.getPaymentReference())
                .paymentProofUrl(buildPaymentProofUrl(m))
                .invoiceAvailable(m.getInvoicePath() != null)
                .build();
    }

    private String buildPaymentProofUrl(Maintenance maintenance) {
        if (maintenance.getPaymentProofFileName() == null || maintenance.getPaymentProofFileName().isBlank()) {
            return null;
        }
        String baseUrl = appBaseUrl == null ? "" : appBaseUrl.trim();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/whistleup/maintenance/payment-proof/" + maintenance.getId()
                + "/" + maintenance.getPaymentProofFileName();
    }

    private String normalizePaymentMethod(String paymentMethod) {
        String normalized = paymentMethod == null ? "" : paymentMethod.trim().toUpperCase(Locale.ENGLISH);
        if (!ALLOWED_PAYMENT_METHODS.contains(normalized)) {
            throw new IllegalArgumentException("Invalid payment method");
        }
        return normalized;
    }

    private String normalizeReference(String transactionReference) {
        if (transactionReference == null) {
            return null;
        }
        String trimmed = transactionReference.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validatePaymentProof(MultipartFile paymentProof) {
        if (paymentProof == null || paymentProof.isEmpty()) {
            return;
        }
        if (paymentProof.getSize() > MAX_PAYMENT_PROOF_BYTES) {
            throw new IllegalArgumentException("Payment proof must be 5 MB or smaller");
        }
        String contentType = paymentProof.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ENGLISH).startsWith("image/")) {
            throw new IllegalArgumentException("Payment proof must be an image");
        }
    }

    public Maintenance getEntity(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("No Maintenance found"));
    }

    public Maintenance getInvoiceByProfileAndPeriod(String profileId, String buildingId, Integer year, Integer month) {
        if (profileId == null || profileId.trim().isEmpty() || buildingId == null || buildingId.trim().isEmpty()
                || year == null || month == null) {
            throw new NotFoundException("No receipt available for the selected combination.");
        }

        Maintenance maintenance = repository.findByProfileIdAndBuildingIdAndMaintenanceYearAndMaintenanceMonth(
                        profileId.trim(),
                        buildingId.trim(),
                        year,
                        month
                )
                .orElseThrow(() -> new NotFoundException("No receipt available for the selected combination."));

        if (maintenance.getStatus() != MaintenanceStatus.PAID) {
            throw new NotFoundException("No receipt available for the selected combination.");
        }

        if (maintenance.getInvoicePath() == null || maintenance.getInvoicePath().isBlank()
                || !Files.exists(Paths.get(maintenance.getInvoicePath()))) {
            String regeneratedPath = invoiceService.generateInvoice(maintenance);
            maintenance.setInvoicePath(regeneratedPath);
            maintenance = repository.save(maintenance);
        }
        return maintenance;
    }

    public List<MaintenanceResponseResource> getMaintenanceByProfileId(String username) {
        return repository.findByProfileIdOrderByMaintenanceYearDescMaintenanceMonthDesc(username)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<Maintenance> getListOfPendingMaintenance() {
        val year = LocalDate.now().getYear();
        return repository.findPendingMaintenanceByPreviousMonthAndYear(year);
    }
}
