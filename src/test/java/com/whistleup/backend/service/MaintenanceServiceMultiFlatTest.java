package com.whistleup.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whistleup.backend.controllers.ResidentsResponse;
import com.whistleup.backend.entity.Maintenance;
import com.whistleup.backend.entity.Profile;
import com.whistleup.backend.repository.BuildingDetailsRepository;
import com.whistleup.backend.repository.MaintenanceRepository;
import com.whistleup.backend.repository.ProfileRepository;
import com.whistleup.backend.resource.MaintenanceCreateResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaintenanceServiceMultiFlatTest {

    @Mock
    private MaintenanceRepository maintenanceRepository;
    @Mock
    private InvoiceService invoiceService;
    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private BuildingDetailsRepository buildingDetailsRepository;
    @Mock
    private NotificationSendService notificationSendService;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private NotebookService notebookService;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MaintenanceService maintenanceService;

    @Test
    void createOrUpdateMaintenance_aggregatesTwoFlatSlotsForSamePhone() {
        String buildingId = "1";
        when(profileRepository.getListOfResidentsByBuilding(1L))
                .thenReturn(
                        List.of(
                                new ResidentsResponse("9100000000", "Owner", "101"),
                                new ResidentsResponse("9100000000", "Owner", "102")));
        when(profileRepository.findDuplicateFlatNosByBuildingId(buildingId)).thenReturn(List.of());
        when(profileRepository.findByPhone("9100000000"))
                .thenReturn(
                        Optional.of(
                                Profile.builder()
                                        .phone("9100000000")
                                        .name("Owner")
                                        .flatNo("101")
                                        .buildingId(buildingId)
                                        .appliancesMaintenanceOptIn(false)
                                        .build()));

        when(maintenanceRepository.findByBuildingIdAndMaintenanceYearAndMaintenanceMonth(
                        eq(buildingId), eq(2026), eq(5)))
                .thenReturn(List.of());
        when(maintenanceRepository.findByProfileIdAndBuildingIdAndMaintenanceYearAndMaintenanceMonth(
                        eq("9100000000"), eq(buildingId), eq(2026), eq(5)))
                .thenReturn(Optional.empty());
        when(maintenanceRepository.save(any(Maintenance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MaintenanceCreateResource req = new MaintenanceCreateResource();
        req.setBuildingId(buildingId);
        req.setYear(2026);
        req.setMonth(5);
        req.setDueDate(LocalDate.of(2026, 5, 15));
        req.setTotalFlats(2);
        req.setAllFlats(List.of("101", "102"));
        req.setAmount(new BigDecimal("1500.00"));
        req.setFixedMaintenance(new BigDecimal("1500.00"));
        req.setWaterMode("FIXED");

        var responses = maintenanceService.createOrUpdateMaintenance(req);

        assertThat(responses).hasSize(1);
        ArgumentCaptor<Maintenance> captor = ArgumentCaptor.forClass(Maintenance.class);
        verify(maintenanceRepository).save(captor.capture());
        Maintenance saved = captor.getValue();
        assertThat(saved.getBilledUnitCount()).isEqualTo(2);
        assertThat(saved.getFixedMaintenance()).isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(saved.getAmount()).isEqualByComparingTo(new BigDecimal("3000.00"));
    }
}
