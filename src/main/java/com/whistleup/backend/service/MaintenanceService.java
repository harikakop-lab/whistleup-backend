package com.whistleup.backend.service;

import com.whistleup.backend.constants.IssueType;
import com.whistleup.backend.constants.MaintenanceStatus;
import com.whistleup.backend.controllers.ResidentsResponse;
import com.whistleup.backend.entity.Maintenance;
import com.whistleup.backend.exception.NotFoundException;
import com.whistleup.backend.repository.MaintenanceRepository;
import com.whistleup.backend.repository.ProfileRepository;
import com.whistleup.backend.resource.MaintenanceCreateResource;
import com.whistleup.backend.resource.MaintenanceResponseResource;
import com.whistleup.backend.scheduler.NotificationScheduler;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private final MaintenanceRepository repository;
    private final InvoiceService invoiceService;
    private final ProfileRepository profileRepository;
    private final NotificationSendService notificationSendService;

    public void createOrUpdateMaintenance(MaintenanceCreateResource maintenanceCreateResource) {
        Optional<Maintenance> maintenanceOptional = repository
                .findByBuildingIdAndMaintenanceYearAndMaintenanceMonth(maintenanceCreateResource.getBuildingId(),
                maintenanceCreateResource.getYear(),
                maintenanceCreateResource.getMonth());
        Maintenance maintenance;
        List<ResidentsResponse> residentsInTheBuilding = profileRepository
                .getListOfResidentsByBuilding(Long.valueOf(maintenanceCreateResource.getBuildingId()));
        if (!CollectionUtils.isEmpty(residentsInTheBuilding)) {
            for (ResidentsResponse residentsResponse : residentsInTheBuilding) {
                val phone = residentsResponse.getPhone();
                val amount = maintenanceCreateResource.getAmount();
                maintenance = maintenanceOptional.orElseGet(() -> Maintenance.builder()
                        .profileId(phone)
                        .maintenanceYear(maintenanceCreateResource.getYear())
                        .maintenanceMonth(maintenanceCreateResource.getMonth())
                        .amount(amount)
                        .dueDate(YearMonth.now().atEndOfMonth())
                        .status(MaintenanceStatus.PENDING)
                        .buildingId(maintenanceCreateResource.getBuildingId())
                        .build());
                maintenance.setAmount(amount);
                repository.save(maintenance);
                notificationSendService.notifyUser(Long.valueOf(phone),
                        "Maintenance",
                        "Please pay your maintenance amount of " + amount + " rupees for this month.",
                        IssueType.ALERT.name());
            }
        }
    }

    public List<MaintenanceResponseResource> getByBuilding(String buildingId) {
        return repository.findByBuildingIdOrderByMaintenanceYearDescMaintenanceMonthDesc(buildingId)
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
                .monthLabel(
                    Month.of(m.getMaintenanceMonth()).name() + " Maintenance"
                )
                .amount(m.getAmount())
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
