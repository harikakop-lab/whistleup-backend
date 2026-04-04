package com.whistleup.backend.service;

import com.whistleup.backend.resource.VisitorEntryCreateRequest;
import com.whistleup.backend.resource.VisitorEntryResponse;

import java.time.LocalDate;
import java.util.List;

public interface VisitorService {

    List<VisitorEntryResponse> listForBuildingAndDate(Long buildingId, LocalDate date);

    VisitorEntryResponse create(Long buildingId, VisitorEntryCreateRequest request);
}
