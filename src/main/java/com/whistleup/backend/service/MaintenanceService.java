package com.whistleup.backend.service;

import com.whistleup.backend.constants.IssueType;
import com.whistleup.backend.constants.MaintenanceStatus;
import com.whistleup.backend.entity.BuildingDetails;
import com.whistleup.backend.entity.Maintenance;
import com.whistleup.backend.entity.Profile;
import com.whistleup.backend.exception.BadRequestException;
import com.whistleup.backend.exception.NotFoundException;
import com.whistleup.backend.repository.BuildingDetailsRepository;
import com.whistleup.backend.repository.MaintenanceRepository;
import com.whistleup.backend.repository.ProfileRepository;
import com.whistleup.backend.controllers.ResidentsResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whistleup.backend.resource.LedgerAttachmentStored;
import com.whistleup.backend.resource.MaintenanceFlatChargeResource;
import com.whistleup.backend.resource.MaintenanceCreateResource;
import com.whistleup.backend.resource.MaintenanceMeterRowResource;
import com.whistleup.backend.resource.MaintenanceAppliancesOptInResource;
import com.whistleup.backend.resource.MaintenanceResponseResource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaintenanceService {

    private final MaintenanceRepository repository;
    private final InvoiceService invoiceService;
    private final ProfileRepository profileRepository;
    private final BuildingDetailsRepository buildingDetailsRepository;
    private final NotificationSendService notificationSendService;
    private final FileStorageService fileStorageService;
    private final NotebookService notebookService;
    private final ObjectMapper objectMapper;

    @Value("${app.base-url}")
    private String appBaseUrl;

    private static final long MAX_PAYMENT_PROOF_BYTES = 5L * 1024 * 1024;
    private static final long MAX_LEDGER_ATTACHMENT_BYTES = 5L * 1024 * 1024;
    private static final int MAX_LEDGER_ATTACHMENTS = 10;
    private static final Set<String> ALLOWED_LEDGER_ATTACHMENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "application/pdf");
    private static final Set<String> ALLOWED_PAYMENT_METHODS = Set.of("UPI", "BANK_TRANSFER", "CASH", "CHEQUE");

    /**
     * Composite key separator for {@code applianceFeesByPhone} entries keyed as
     * {@code phone|||flat}.
     */
    private static final String APPLIANCE_FEE_KEY_DELIM = "|||";

    private record BillableSlot(String phone, String name, String flatNoRaw) {
    }

    public List<MaintenanceResponseResource> createOrUpdateMaintenance(
            MaintenanceCreateResource maintenanceCreateResource) {
        return createOrUpdateMaintenance(maintenanceCreateResource, null);
    }

    @Transactional
    public List<MaintenanceResponseResource> createOrUpdateMaintenance(
            MaintenanceCreateResource maintenanceCreateResource,
            MultipartFile[] ledgerAttachmentFiles) {
        String priorAttachmentsJson = snapshotLedgerAttachmentsJson(maintenanceCreateResource);
        List<MaintenanceResponseResource> rows = upsertMaintenanceRows(maintenanceCreateResource);
        if (!CollectionUtils.isEmpty(rows)) {
            applyLedgerAttachmentChanges(maintenanceCreateResource, ledgerAttachmentFiles, priorAttachmentsJson);
        }
        triggerNotebookRefreshAfterMaintenance(rows, maintenanceCreateResource);
        return rows;
    }

    public List<MaintenanceResponseResource> updateMaintenance(MaintenanceCreateResource maintenanceCreateResource) {
        return updateMaintenance(maintenanceCreateResource, null);
    }

    @Transactional
    public List<MaintenanceResponseResource> updateMaintenance(
            MaintenanceCreateResource maintenanceCreateResource,
            MultipartFile[] ledgerAttachmentFiles) {
        String priorAttachmentsJson = snapshotLedgerAttachmentsJson(maintenanceCreateResource);
        List<MaintenanceResponseResource> rows = upsertMaintenanceRows(maintenanceCreateResource);
        if (!CollectionUtils.isEmpty(rows)) {
            applyLedgerAttachmentChanges(maintenanceCreateResource, ledgerAttachmentFiles, priorAttachmentsJson);
        }
        triggerNotebookRefreshAfterMaintenance(rows, maintenanceCreateResource);
        return rows;
    }

    private String snapshotLedgerAttachmentsJson(MaintenanceCreateResource req) {
        if (req.getBuildingId() == null
                || req.getBuildingId().isBlank()
                || req.getYear() == null
                || req.getMonth() == null) {
            return null;
        }
        return repository
                .findByBuildingIdAndMaintenanceYearAndMaintenanceMonth(
                        req.getBuildingId(), req.getYear(), req.getMonth())
                .stream()
                .map(Maintenance::getLedgerAttachmentsJson)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .findFirst()
                .orElse(null);
    }

    private void applyLedgerAttachmentChanges(
            MaintenanceCreateResource req,
            MultipartFile[] ledgerAttachmentFiles,
            String priorAttachmentsJson) {
        if (ledgerAttachmentFiles == null) {
            return;
        }
        int nonEmpty = 0;
        for (MultipartFile f : ledgerAttachmentFiles) {
            if (f != null && !f.isEmpty()) {
                nonEmpty++;
            }
        }
        if (nonEmpty == 0) {
            return;
        }
        if (nonEmpty > MAX_LEDGER_ATTACHMENTS) {
            throw new BadRequestException(
                    "Too many attachments",
                    "You can attach at most " + MAX_LEDGER_ATTACHMENTS + " files.");
        }
        List<LedgerAttachmentStored> previous = parseStoredAttachments(priorAttachmentsJson);
        if (!previous.isEmpty()) {
            fileStorageService.deleteMaintenanceLedgerStoredFiles(
                    req.getBuildingId(),
                    req.getYear(),
                    req.getMonth(),
                    previous.stream().map(LedgerAttachmentStored::getFileName).toList());
        }
        List<LedgerAttachmentStored> saved = new ArrayList<>();
        for (MultipartFile file : ledgerAttachmentFiles) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            validateLedgerAttachmentFile(file);
            String storedName = fileStorageService.saveMaintenanceLedgerAttachment(
                    req.getBuildingId(), req.getYear(), req.getMonth(), file);
            saved.add(
                    LedgerAttachmentStored.builder()
                            .fileName(storedName)
                            .contentType(normalizeLedgerAttachmentContentType(file))
                            .build());
        }
        if (saved.isEmpty()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(saved);
            repository.updateLedgerAttachmentsJsonForPeriod(
                    req.getBuildingId(), req.getYear(), req.getMonth(), json);
            repository.flush();
        } catch (Exception e) {
            throw new BadRequestException("Could not persist attachment metadata", e.getMessage());
        }
    }

    private List<LedgerAttachmentStored> parseStoredAttachments(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<LedgerAttachmentStored> list = objectMapper.readValue(json,
                    new TypeReference<List<LedgerAttachmentStored>>() {
                    });
            return list == null ? List.of() : list;
        } catch (Exception e) {
            log.warn("Could not parse ledger_attachments_json: {}", e.toString());
            return List.of();
        }
    }

    private void validateLedgerAttachmentFile(MultipartFile file) {
        if (file.getSize() > MAX_LEDGER_ATTACHMENT_BYTES) {
            throw new BadRequestException(
                    "Attachment too large", "Each file must be at most 5 MB.");
        }
        String ct = normalizeLedgerAttachmentContentType(file);
        if (!ALLOWED_LEDGER_ATTACHMENT_TYPES.contains(ct)) {
            throw new BadRequestException(
                    "Unsupported file type",
                    "Allowed types: JPEG, PNG, WebP, PDF.");
        }
    }

    private String normalizeLedgerAttachmentContentType(MultipartFile file) {
        String raw = file.getContentType();
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String lower = raw.split(";")[0].trim().toLowerCase(Locale.ROOT);
        if ("image/jpg".equals(lower)) {
            return "image/jpeg";
        }
        return lower;
    }

    private void triggerNotebookRefreshAfterMaintenance(
            List<MaintenanceResponseResource> rows,
            MaintenanceCreateResource req) {
        if (CollectionUtils.isEmpty(rows) || req == null || req.getBuildingId() == null || req.getBuildingId().isBlank()
                || req.getYear() == null || req.getMonth() == null) {
            return;
        }
        try {
            notebookService.refreshNotebookFromMaintenance(req.getBuildingId(), req.getYear(), req.getMonth());
        } catch (Exception ex) {
            log.warn(
                    "refreshNotebookFromMaintenance failed for buildingId={} {}-{}: {}",
                    req.getBuildingId(),
                    req.getYear(),
                    req.getMonth(),
                    ex.toString());
        }
    }

    private List<MaintenanceResponseResource> upsertMaintenanceRows(
            MaintenanceCreateResource maintenanceCreateResource) {
        assertNoDuplicateFlatAssignments(maintenanceCreateResource.getBuildingId());

        Map<String, BigDecimal> explicitAmountByFlat = toExplicitAmountByFlat(
                maintenanceCreateResource.getFlatCharges());
        Map<String, BigDecimal> explicitAppliancesByFlat = toExplicitAppliancesByFlat(
                maintenanceCreateResource.getFlatCharges());
        Map<String, BigDecimal> explicitAppliancesByPhone = toExplicitAppliancesByPhone(
                maintenanceCreateResource.getApplianceFeesByPhone());
        BuildingDetails buildingDetails = resolveBuilding(maintenanceCreateResource.getBuildingId());

        List<BillableSlot> billableSlots = resolveBillableSlots(
                maintenanceCreateResource.getBuildingId(),
                maintenanceCreateResource.getAllFlats(),
                explicitAmountByFlat.keySet());
        if (CollectionUtils.isEmpty(billableSlots)) {
            List<Profile> residentsInTheBuilding = profileRepository
                    .findByBuildingId(maintenanceCreateResource.getBuildingId());
            if (CollectionUtils.isEmpty(residentsInTheBuilding)) {
                return List.of();
            }
            List<Profile> billableProfiles = resolveBillableProfiles(
                    residentsInTheBuilding,
                    maintenanceCreateResource.getAllFlats(),
                    explicitAmountByFlat.keySet());
            if (CollectionUtils.isEmpty(billableProfiles)) {
                return List.of();
            }
            billableSlots = billableProfiles.stream()
                    .map(p -> new BillableSlot(p.getPhone(), p.getName(), p.getFlatNo()))
                    .toList();
        }

        int residentCountForSplit = Math.max(resolveTotalFlats(maintenanceCreateResource, billableSlots.size()), 1);
        List<String> flatsForWater = orderedFlatsForWater(maintenanceCreateResource, billableSlots);

        BigDecimal sharedExpenseTotal = resolveSharedExpenseTotal(maintenanceCreateResource);
        BigDecimal defaultWaterPerFlat = resolveDefaultWaterPerFlat(maintenanceCreateResource, residentCountForSplit);
        Map<String, BigDecimal> waterByFlat = resolveWaterByFlat(
                maintenanceCreateResource, flatsForWater, billableSlots, defaultWaterPerFlat);
        BigDecimal basePerFlat = sharedExpenseTotal.divide(BigDecimal.valueOf(residentCountForSplit), 2,
                RoundingMode.HALF_UP);
        BigDecimal watchmanPerFlat = splitPerFlat(maintenanceCreateResource.getWatchmanSalary(), residentCountForSplit);
        BigDecimal garbagePerFlat = splitPerFlat(maintenanceCreateResource.getGarbageCollection(),
                residentCountForSplit);
        BigDecimal liftPerFlat = splitPerFlat(maintenanceCreateResource.getLiftMaintenance(), residentCountForSplit);
        BigDecimal electricityPerFlat = splitPerFlat(maintenanceCreateResource.getElectricityCommon(),
                residentCountForSplit);
        BigDecimal motorPerFlat = splitPerFlat(maintenanceCreateResource.getMotorPump(), residentCountForSplit);
        BigDecimal miscPerFlat = splitPerFlat(maintenanceCreateResource.getMiscellaneous(), residentCountForSplit);
        Map<String, BigDecimal> customExpensesPerFlat = splitCustomExpensesPerFlat(
                maintenanceCreateResource.getCustomExpenses(),
                residentCountForSplit);

        boolean applianceAware = buildingDetails != null && buildingDetails.isAppliancesNeeded();
        BigDecimal appliancePool = Objects.requireNonNullElse(maintenanceCreateResource.getAppliancesTotalAmount(),
                BigDecimal.ZERO);
        Map<String, Profile> profileByPhone = loadProfilesForSlots(billableSlots);
        List<BillableSlot> optedSlots = billableSlots.stream()
                .filter(s -> isApplianceOptedIn(profileByPhone.get(s.phone())))
                .toList();
        int optCount = optedSlots.size();
        BigDecimal perApplianceShare = (applianceAware && optCount > 0 && appliancePool.compareTo(BigDecimal.ZERO) > 0)
                ? appliancePool.divide(BigDecimal.valueOf(optCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        Set<String> optedPhones = new HashSet<>(optedSlots.stream().map(BillableSlot::phone).toList());
        Map<String, Long> optedCountByFlat = optedSlots.stream()
                .map(s -> normalizeFlatNo(s.flatNoRaw()))
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(f -> f, Collectors.counting()));

        Map<String, List<BillableSlot>> slotsByPhone = billableSlots.stream()
                .collect(Collectors.groupingBy(BillableSlot::phone, LinkedHashMap::new, Collectors.toList()));

        List<Maintenance> savedRows = new ArrayList<>();
        for (Map.Entry<String, List<BillableSlot>> entry : slotsByPhone.entrySet()) {
            String phone = entry.getKey();
            List<BillableSlot> phoneSlots = entry.getValue();
            Profile profile = resolveProfileForBilling(phone, phoneSlots.get(0), profileByPhone);
            int k = phoneSlots.size();
            List<String> flatsForPhone = phoneSlots.stream()
                    .map(s -> normalizeFlatNo(s.flatNoRaw()))
                    .filter(Objects::nonNull)
                    .toList();
            int optedSlotsForPhone = (int) phoneSlots.stream()
                    .filter(s -> optedPhones.contains(s.phone()) && isApplianceOptedIn(profileByPhone.get(s.phone())))
                    .count();
            savedRows.add(
                    upsertMaintenanceRowAggregated(
                            maintenanceCreateResource,
                            profile,
                            residentCountForSplit,
                            k,
                            flatsForPhone,
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
                            perApplianceShare,
                            optedSlotsForPhone));
        }

        notifyResidents(savedRows);
        return savedRows.stream().map(this::toResponse).toList();
    }

    private static boolean isApplianceOptedIn(Profile p) {
        return p != null
                && Boolean.TRUE.equals(p.getAppliancesMaintenanceOptIn())
                && p.getAppliancesJson() != null
                && !p.getAppliancesJson().isBlank();
    }

    private Map<String, Profile> loadProfilesForSlots(List<BillableSlot> billableSlots) {
        Map<String, Profile> map = new LinkedHashMap<>();
        for (BillableSlot s : billableSlots) {
            if (s.phone() == null || s.phone().isBlank()) {
                continue;
            }
            map.computeIfAbsent(s.phone().trim(), ph -> profileRepository.findByPhone(ph).orElse(null));
        }
        return map;
    }

    private Profile resolveProfileForBilling(String phone, BillableSlot firstSlot,
            Map<String, Profile> profileByPhone) {
        Profile loaded = profileByPhone.get(phone);
        if (loaded != null) {
            return loaded;
        }
        return Profile.builder()
                .phone(phone)
                .name(firstSlot.name())
                .flatNo(firstSlot.flatNoRaw())
                .build();
    }

    private List<BillableSlot> resolveBillableSlots(String buildingId, List<String> allFlats,
            Set<String> explicitFlatKeys) {
        Set<String> requestedFlats = new HashSet<>();
        if (!CollectionUtils.isEmpty(allFlats)) {
            allFlats.stream()
                    .map(this::normalizeFlatNo)
                    .filter(Objects::nonNull)
                    .forEach(requestedFlats::add);
        }
        if (!CollectionUtils.isEmpty(explicitFlatKeys)) {
            explicitFlatKeys.stream()
                    .map(this::normalizeFlatNo)
                    .filter(Objects::nonNull)
                    .forEach(requestedFlats::add);
        }

        List<BillableSlot> out = new ArrayList<>();
        try {
            List<ResidentsResponse> flatResidents = profileRepository
                    .getListOfResidentsByBuilding(Long.valueOf(buildingId.trim()));
            if (!CollectionUtils.isEmpty(flatResidents)) {
                for (ResidentsResponse r : flatResidents) {
                    if (r.getPhone() == null || r.getPhone().isBlank()) {
                        continue;
                    }
                    String nf = normalizeFlatNo(r.getFlatNo());
                    if (nf == null) {
                        continue;
                    }
                    if (!requestedFlats.isEmpty() && !requestedFlats.contains(nf)) {
                        continue;
                    }
                    out.add(new BillableSlot(r.getPhone().trim(), r.getName(), r.getFlatNo()));
                }
            }
        } catch (Exception ex) {
            log.warn("resolveBillableSlots: could not load flat residents for buildingId={}: {}", buildingId,
                    ex.toString());
        }
        return out;
    }

    private List<String> orderedFlatsForWater(MaintenanceCreateResource req, List<BillableSlot> slots) {
        if (!CollectionUtils.isEmpty(req.getAllFlats())) {
            return req.getAllFlats().stream()
                    .map(this::normalizeFlatNo)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
        }
        return slots.stream()
                .map(s -> normalizeFlatNo(s.flatNoRaw()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
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

    private List<Profile> resolveBillableProfiles(
            List<Profile> residentsInTheBuilding,
            List<String> allFlats,
            Set<String> explicitFlatKeys) {
        Set<String> requestedFlats = new HashSet<>();
        if (!CollectionUtils.isEmpty(allFlats)) {
            allFlats.stream()
                    .map(this::normalizeFlatNo)
                    .filter(Objects::nonNull)
                    .forEach(requestedFlats::add);
        }
        if (!CollectionUtils.isEmpty(explicitFlatKeys)) {
            explicitFlatKeys.stream()
                    .map(this::normalizeFlatNo)
                    .filter(Objects::nonNull)
                    .forEach(requestedFlats::add);
        }

        Map<String, Profile> byFlat = new LinkedHashMap<>();
        for (Profile profile : residentsInTheBuilding) {
            String normalizedFlat = normalizeFlatNo(profile.getFlatNo());
            if (normalizedFlat == null) {
                continue;
            }
            if (!requestedFlats.isEmpty() && !requestedFlats.contains(normalizedFlat)) {
                continue;
            }
            byFlat.putIfAbsent(normalizedFlat, profile);
        }
        return new ArrayList<>(byFlat.values());
    }

    public List<MaintenanceAppliancesOptInResource> listAppliancesOptInFlats(String buildingId) {
        List<BillableSlot> slots = resolveBillableSlots(buildingId, List.of(), Set.of());
        if (CollectionUtils.isEmpty(slots)) {
            List<Profile> residents = profileRepository.findByBuildingId(buildingId);
            if (CollectionUtils.isEmpty(residents)) {
                return List.of();
            }
            slots = residents.stream()
                    .map(p -> new BillableSlot(p.getPhone(), p.getName(), p.getFlatNo()))
                    .toList();
        }
        Map<String, Profile> profileByPhone = loadProfilesForSlots(slots);
        List<MaintenanceAppliancesOptInResource> out = new ArrayList<>();
        for (BillableSlot s : slots) {
            Profile p = profileByPhone.get(s.phone());
            if (!isApplianceOptedIn(p)) {
                continue;
            }
            out.add(
                    MaintenanceAppliancesOptInResource.builder()
                            .phone(s.phone())
                            .name(s.name() != null && !s.name().isBlank() ? s.name() : p.getName())
                            .flatNo(s.flatNoRaw())
                            .appliancesJson(p.getAppliancesJson())
                            .build());
        }
        return out;
    }

    private Maintenance upsertMaintenanceRowAggregated(
            MaintenanceCreateResource req,
            Profile profile,
            int billedUnitCount,
            int flatSlotCountForPhone,
            List<String> normalizedFlatsForPhone,
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
            BigDecimal perApplianceShare,
            int optedSlotCountForPhone) {
        String phone = profile.getPhone();
        BigDecimal flatMultiplier = BigDecimal.valueOf(Math.max(1, flatSlotCountForPhone));

        Optional<Maintenance> existingOpt = repository
                .findByProfileIdAndBuildingIdAndMaintenanceYearAndMaintenanceMonth(
                        phone,
                        req.getBuildingId(),
                        req.getYear(),
                        req.getMonth());
        boolean existingRow = existingOpt.isPresent();

        LinkedHashMap<String, BigDecimal> scaledCustomPerPhone = scaleCustomMap(customExpensesPerFlat, flatMultiplier);
        LinkedHashMap<String, BigDecimal> customToStore = resolveCustomExpensesToStore(req, existingOpt,
                scaledCustomPerPhone);
        BigDecimal rowCustomTotal = sumMapDecimalValues(customToStore);

        BigDecimal explicitTotalForPhone = BigDecimal.ZERO;
        for (String f : normalizedFlatsForPhone) {
            BigDecimal a = explicitAmountByFlat.get(f);
            if (a != null && a.compareTo(BigDecimal.ZERO) > 0) {
                explicitTotalForPhone = explicitTotalForPhone.add(a);
            }
        }
        BigDecimal resolvedAmount = explicitTotalForPhone.compareTo(BigDecimal.ZERO) > 0 ? explicitTotalForPhone : null;
        boolean hasExplicitFlatTotal = resolvedAmount != null;

        BigDecimal defaultWater = Objects.requireNonNullElse(defaultWaterPerFlat, BigDecimal.ZERO);
        BigDecimal incomingWater = BigDecimal.ZERO;
        for (String f : normalizedFlatsForPhone) {
            incomingWater = incomingWater.add(waterByFlat.getOrDefault(f, defaultWater));
        }
        BigDecimal waterForPhone = MaintenanceUpsertMerge.resolveWaterForFlat(
                incomingWater,
                existingRow,
                existingOpt.map(Maintenance::getWaterAmount).orElse(null),
                MaintenanceUpsertMerge.hasWaterPayload(req));

        BigDecimal applianceShare = resolveApplianceShareForPhone(
                phone,
                normalizedFlatsForPhone,
                explicitAppliancesByPhone,
                explicitAppliancesByFlat,
                optedAppliancePhones,
                optedCountByFlat,
                perApplianceShare,
                optedSlotCountForPhone);

        BigDecimal baseForPhone = basePerFlat.multiply(flatMultiplier);
        if (!hasExplicitFlatTotal) {
            BigDecimal effectiveBase = MaintenanceUpsertMerge.resolveEffectiveBase(
                    baseForPhone,
                    existingRow,
                    existingOpt.map(Maintenance::getAmount).orElse(null),
                    existingOpt.map(Maintenance::getWaterAmount).orElse(null),
                    existingOpt.map(Maintenance::getAppliancesAmount).orElse(null),
                    MaintenanceUpsertMerge.blocksPreservedSharedBaseMerge(req));
            BigDecimal land = effectiveBase;
            if (existingRow && existingOpt.isPresent()) {
                land = land.max(floorLandFromExisting(existingOpt.get(), rowCustomTotal));
            }
            resolvedAmount = land.add(waterForPhone).add(applianceShare);
        } else if (existingRow && existingOpt.isPresent()) {
            BigDecimal floor = floorLandFromExisting(existingOpt.get(), rowCustomTotal);
            BigDecimal reconciled = floor.add(waterForPhone).add(applianceShare);
            resolvedAmount = Objects.requireNonNullElse(resolvedAmount, BigDecimal.ZERO).max(reconciled);
        }

        if (resolvedAmount != null && resolvedAmount.compareTo(BigDecimal.ZERO) <= 0 && req.getAmount() != null) {
            resolvedAmount = req.getAmount().multiply(flatMultiplier);
        }

        BigDecimal fixedMaintenance;
        if (req.getFixedMaintenance() != null && req.getFixedMaintenance().compareTo(BigDecimal.ZERO) > 0) {
            fixedMaintenance = req.getFixedMaintenance().multiply(flatMultiplier);
        } else if (existingOpt.isPresent()) {
            Maintenance ex = existingOpt.get();
            if (ex.getFixedMaintenance() != null && ex.getFixedMaintenance().compareTo(BigDecimal.ZERO) > 0) {
                fixedMaintenance = ex.getFixedMaintenance();
            } else {
                fixedMaintenance = Objects.requireNonNullElse(req.getAmount(), basePerFlat).multiply(flatMultiplier);
            }
        } else {
            fixedMaintenance = Objects.requireNonNullElse(req.getAmount(), basePerFlat).multiply(flatMultiplier);
        }

        final BigDecimal amountToPersist = resolvedAmount;
        YearMonth billingPeriod = YearMonth.of(req.getYear(), req.getMonth());
        Maintenance maintenance = existingOpt
                .orElseGet(() -> Maintenance.builder()
                        .profileId(phone)
                        .maintenanceYear(req.getYear())
                        .maintenanceMonth(req.getMonth())
                        .amount(amountToPersist)
                        .dueDate(billingPeriod.atEndOfMonth())
                        .status(MaintenanceStatus.PENDING)
                        .buildingId(req.getBuildingId())
                        .build());
        maintenance.setAmount(amountToPersist);
        maintenance.setFixedMaintenance(fixedMaintenance);
        maintenance.setBilledUnitCount(billedUnitCount);
        if (Objects.nonNull(req.getDueDate())) {
            maintenance.setDueDate(req.getDueDate());
        } else if (maintenance.getDueDate() == null) {
            maintenance.setDueDate(billingPeriod.atEndOfMonth());
        }
        maintenance.setWaterMode(normalizeMode(req.getWaterMode()));
        maintenance.setWatchmanSalary(scaleBy(watchmanPerFlat, flatMultiplier));
        maintenance.setGarbageCollection(scaleBy(garbagePerFlat, flatMultiplier));
        maintenance.setLiftMaintenance(scaleBy(liftPerFlat, flatMultiplier));
        maintenance.setElectricityCommon(scaleBy(electricityPerFlat, flatMultiplier));
        maintenance.setMotorPump(scaleBy(motorPerFlat, flatMultiplier));
        maintenance.setMiscellaneous(scaleBy(miscPerFlat, flatMultiplier));
        maintenance.setCustomExpenses(customToStore);
        maintenance.setWaterAmount(waterForPhone);
        maintenance.setAppliancesAmount(applianceShare);
        maintenance.setAssetAmount(resolveAssetAmountToPersist(req, existingOpt));
        maintenance.setAssetDescription(resolveAssetDescriptionToPersist(req, existingOpt));
        return repository.save(maintenance);
    }

    private static BigDecimal scaleBy(BigDecimal perFlat, BigDecimal multiplier) {
        if (perFlat == null || perFlat.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return perFlat.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    private static LinkedHashMap<String, BigDecimal> scaleCustomMap(
            Map<String, BigDecimal> perFlat, BigDecimal multiplier) {
        LinkedHashMap<String, BigDecimal> out = new LinkedHashMap<>();
        if (perFlat == null || perFlat.isEmpty()) {
            return out;
        }
        for (Map.Entry<String, BigDecimal> e : perFlat.entrySet()) {
            if (e.getKey() == null) {
                continue;
            }
            BigDecimal v = Objects.requireNonNullElse(e.getValue(), BigDecimal.ZERO);
            if (v.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            out.put(e.getKey(), v.multiply(multiplier).setScale(2, RoundingMode.HALF_UP));
        }
        return out;
    }

    private BigDecimal resolveApplianceShareForPhone(
            String phone,
            List<String> normalizedFlatsForPhone,
            Map<String, BigDecimal> explicitAppliancesByPhone,
            Map<String, BigDecimal> explicitAppliancesByFlat,
            Set<String> optedAppliancePhones,
            Map<String, Long> optedCountByFlat,
            BigDecimal perApplianceShare,
            int optedSlotCountForPhone) {
        if (explicitAppliancesByPhone != null && !explicitAppliancesByPhone.isEmpty()) {
            return explicitAppliancesByPhone.getOrDefault(phone, BigDecimal.ZERO);
        }
        BigDecimal fromFlat = BigDecimal.ZERO;
        for (String flatNo : normalizedFlatsForPhone) {
            if (!explicitAppliancesByFlat.containsKey(flatNo)) {
                continue;
            }
            if (!optedAppliancePhones.contains(phone)) {
                continue;
            }
            BigDecimal pool = Objects.requireNonNullElse(explicitAppliancesByFlat.get(flatNo), BigDecimal.ZERO);
            long n = optedCountByFlat.getOrDefault(flatNo, 1L);
            fromFlat = fromFlat.add(
                    pool.divide(BigDecimal.valueOf(Math.max(1L, n)), 2, RoundingMode.HALF_UP));
        }
        if (fromFlat.compareTo(BigDecimal.ZERO) > 0) {
            return fromFlat;
        }
        if (optedAppliancePhones.contains(phone) && optedSlotCountForPhone > 0) {
            return Objects.requireNonNullElse(perApplianceShare, BigDecimal.ZERO)
                    .multiply(BigDecimal.valueOf(optedSlotCountForPhone))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    static BigDecimal resolveAssetAmountToPersist(MaintenanceCreateResource req, Optional<Maintenance> existingOpt) {
        BigDecimal incomingAsset = Objects.requireNonNullElse(req.getAssetAmount(), BigDecimal.ZERO);
        if (incomingAsset.compareTo(BigDecimal.ZERO) < 0) {
            incomingAsset = BigDecimal.ZERO;
        }
        BigDecimal existingAsset = existingOpt
                .map(Maintenance::getAssetAmount)
                .map(v -> v.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : v)
                .orElse(BigDecimal.ZERO);
        return existingAsset.add(incomingAsset);
    }

    static String resolveAssetDescriptionToPersist(MaintenanceCreateResource req, Optional<Maintenance> existingOpt) {
        String incoming = req.getAssetDescription() == null ? "" : req.getAssetDescription().trim();
        if (!incoming.isBlank()) {
            return incoming;
        }
        return existingOpt
                .map(Maintenance::getAssetDescription)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .orElse(null);
    }

    private BigDecimal splitPerFlat(BigDecimal value, int totalProfiles) {
        if (value == null || totalProfiles <= 0) {
            return BigDecimal.ZERO;
        }
        return value.divide(BigDecimal.valueOf(totalProfiles), 2, RoundingMode.HALF_UP);
    }

    private void notifyResidents(List<Maintenance> rows) {
        for (Maintenance row : rows) {
            try {
                notificationSendService.notifyUser(
                Long.valueOf(row.getProfileId()),
                "Maintenance",
                "Please pay your maintenance amount of " + row.getAmount() + " rupees for " +
                YearMonth.of(row.getMaintenanceYear(), row.getMaintenanceMonth()).toString() + ".",
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
     * Per-flat appliance amounts from the client. Only flats with a non-null
     * {@code appliancesAmount} on a charge row
     * participate; duplicate flat keys are merged by sum. When the map is empty for
     * all rows, callers fall back to
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
            String key = e.getKey().trim();
            if (key.isEmpty()) {
                continue;
            }
            String phone;
            if (key.contains(APPLIANCE_FEE_KEY_DELIM)) {
                int idx = key.indexOf(APPLIANCE_FEE_KEY_DELIM);
                phone = key.substring(0, idx).trim();
            } else {
                phone = key;
            }
            if (phone.isEmpty()) {
                continue;
            }
            BigDecimal amt = Objects.requireNonNullElse(e.getValue(), BigDecimal.ZERO);
            byPhone.merge(phone, amt, BigDecimal::add);
        }
        return byPhone;
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
                    entry.getValue().divide(BigDecimal.valueOf(totalProfiles), 2, RoundingMode.HALF_UP));
        }
        return perFlat;
    }

    private LinkedHashMap<String, BigDecimal> resolveCustomExpensesToStore(
            MaintenanceCreateResource req,
            Optional<Maintenance> existingOpt,
            Map<String, BigDecimal> customExpensesPerFlat) {
        LinkedHashMap<String, BigDecimal> out = new LinkedHashMap<>();
        if (MaintenanceUpsertMerge.hasCustomExpensePayload(req)) {
            out.putAll(customExpensesPerFlat);
        } else if (existingOpt.isPresent() && existingOpt.get().getCustomExpenses() != null) {
            out.putAll(existingOpt.get().getCustomExpenses());
        } else {
            out.putAll(customExpensesPerFlat);
        }
        return out;
    }

    private BigDecimal sumMapDecimalValues(Map<String, BigDecimal> m) {
        if (m == null || m.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return m.values().stream()
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Lower bound for land (amount − water − appliances) from the prior row so
     * incremental updates
     * (notably explicit {@code flatCharges} totals that omit custom) cannot drop
     * persisted custom charges.
     */
    private BigDecimal floorLandFromExisting(Maintenance ex, BigDecimal perFlatCustomTotal) {
        if (ex == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal impliedPrior = Objects.requireNonNullElse(ex.getAmount(), BigDecimal.ZERO)
                .subtract(Objects.requireNonNullElse(ex.getWaterAmount(), BigDecimal.ZERO))
                .subtract(Objects.requireNonNullElse(ex.getAppliancesAmount(), BigDecimal.ZERO));
        if (impliedPrior.compareTo(BigDecimal.ZERO) < 0) {
            impliedPrior = BigDecimal.ZERO;
        }
        BigDecimal fixedCol = Objects.requireNonNullElse(ex.getFixedMaintenance(), BigDecimal.ZERO);
        if (fixedCol.compareTo(BigDecimal.ZERO) < 0) {
            fixedCol = BigDecimal.ZERO;
        }
        BigDecimal custom = Objects.requireNonNullElse(perFlatCustomTotal, BigDecimal.ZERO);
        if (custom.compareTo(BigDecimal.ZERO) < 0) {
            custom = BigDecimal.ZERO;
        }
        return impliedPrior.max(fixedCol.add(custom));
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
            List<String> flatsForWaterLoop,
            List<BillableSlot> billableSlots,
            BigDecimal defaultWaterPerFlat) {
        Map<String, BigDecimal> waterByFlat = new HashMap<>();
        String mode = normalizeMode(req.getWaterMode());
        if ("INDIVIDUAL".equals(mode)) {
            BigDecimal rate = Objects.requireNonNullElse(req.getIndividualRatePerUnit(), BigDecimal.ZERO);
            List<MaintenanceMeterRowResource> rows = req.getIndividualRows() == null ? List.of()
                    : req.getIndividualRows();
            for (MaintenanceMeterRowResource row : rows) {
                if (row == null || row.getFlatNumber() == null)
                    continue;
                BigDecimal units = Objects.requireNonNullElse(row.getUnits(), BigDecimal.ZERO);
                waterByFlat.put(normalizeFlatNo(row.getFlatNumber()), units.multiply(rate));
            }
            return waterByFlat;
        }
        if ("MIXED".equals(mode)) {
            BigDecimal mixedRate = Objects.requireNonNullElse(req.getMixedRatePerUnit(), BigDecimal.ZERO);
            List<MaintenanceMeterRowResource> mixedRows = req.getMixedMeterRows() == null ? List.of()
                    : req.getMixedMeterRows();
            for (MaintenanceMeterRowResource row : mixedRows) {
                if (row == null || row.getFlatNumber() == null)
                    continue;
                BigDecimal units = Objects.requireNonNullElse(row.getUnits(), BigDecimal.ZERO);
                waterByFlat.put(normalizeFlatNo(row.getFlatNumber()), units.multiply(mixedRate));
            }

            List<String> allFlats = req.getAllFlats();
            if (CollectionUtils.isEmpty(allFlats)) {
                allFlats = billableSlots.stream()
                        .map(s -> normalizeFlatNo(s.flatNoRaw()))
                        .filter(Objects::nonNull)
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
        for (String flat : flatsForWaterLoop) {
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

    private void assertNoDuplicateFlatAssignments(String buildingId) {
        if (buildingId == null || buildingId.isBlank()) {
            return;
        }
        List<String> duplicates = profileRepository.findDuplicateFlatNosByBuildingId(buildingId);
        if (duplicates == null || duplicates.isEmpty()) {
            return;
        }
        throw new BadRequestException(
                "Duplicate flat assignments found",
                "Resolve duplicate profiles for flat(s): " + String.join(", ", duplicates)
                        + " before generating maintenance.");
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
                        Month.of(m.getMaintenanceMonth()).name() + " Maintenance")
                .amount(m.getAmount())
                .fixedMaintenance(m.getFixedMaintenance())
                .watchmanSalary(m.getWatchmanSalary())
                .garbageCollection(m.getGarbageCollection())
                .liftMaintenance(m.getLiftMaintenance())
                .electricityCommon(m.getElectricityCommon())
                .motorPump(m.getMotorPump())
                .miscellaneous(m.getMiscellaneous())
                .customExpenses(m.getCustomExpenses())
                .waterAmount(m.getWaterAmount())
                .appliancesAmount(m.getAppliancesAmount())
                .assetAmount(m.getAssetAmount())
                .assetDescription(m.getAssetDescription())
                .waterMode(m.getWaterMode())
                .dueDate(m.getDueDate())
                .status(m.getStatus())
                .paidDate(m.getPaidDate())
                .paymentMethod(m.getPaymentMethod())
                .paymentReference(m.getPaymentReference())
                .paymentProofUrl(buildPaymentProofUrl(m))
                .invoiceAvailable(m.getInvoicePath() != null)
                .billedUnitCount(m.getBilledUnitCount())
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
                month)
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
