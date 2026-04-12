package com.whistleup.backend.service;

import com.whistleup.backend.resource.QrBuildingMappingResponse;
import com.whistleup.backend.resource.QrBuildingMappingUpsertRequest;

public interface QrBuildingMappingService {
    QrBuildingMappingResponse upsert(QrBuildingMappingUpsertRequest request);

    QrBuildingMappingResponse getByToken(String token);
}
