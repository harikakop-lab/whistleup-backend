package com.whistleup.backend.service;

import com.whistleup.backend.constants.MaintenanceStatus;
import com.whistleup.backend.entity.Maintenance;
import com.whistleup.backend.exception.NotFoundException;
import com.whistleup.backend.repository.MaintenanceRepository;
import com.whistleup.backend.resource.MaintenanceCreateResource;
import com.whistleup.backend.resource.MaintenanceResponseResource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private final MaintenanceRepository repository;
    private final InvoiceService invoiceService;

    public void createMaintenance(MaintenanceCreateResource req) {

        repository.findByProfileIdAndMaintenanceYearAndMaintenanceMonth(
                req.getProfileId(), req.getYear(), req.getMonth()
        ).ifPresent(m -> {
            throw new IllegalStateException("Maintenance already exists");
        });

        Maintenance maintenance = Maintenance.builder()
                .profileId(req.getProfileId())
                .maintenanceYear(req.getYear())
                .maintenanceMonth(req.getMonth())
                .amount(req.getAmount())
                .dueDate(req.getDueDate())
                .status(MaintenanceStatus.PENDING)
                .build();

        repository.save(maintenance);
    }

    public List<MaintenanceResponseResource> getByProfile(String profileId) {

        return repository.findByProfileIdOrderByMaintenanceYearDescMaintenanceMonthDesc(profileId)
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
}
