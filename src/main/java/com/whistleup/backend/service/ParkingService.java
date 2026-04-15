package com.whistleup.backend.service;

import com.whistleup.backend.resource.ParkingAllocationCreateBatchRequest;
import com.whistleup.backend.resource.ParkingAllocationResponse;

import java.util.List;

public interface ParkingService {

    List<ParkingAllocationResponse> listByBuilding(Long buildingId);

    List<ParkingAllocationResponse> createAllocations(Long buildingId, ParkingAllocationCreateBatchRequest request);
}
