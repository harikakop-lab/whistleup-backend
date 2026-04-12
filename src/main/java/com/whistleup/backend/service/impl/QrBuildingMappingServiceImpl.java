package com.whistleup.backend.service.impl;

import com.whistleup.backend.entity.BuildingDetails;
import com.whistleup.backend.entity.QrBuildingMapping;
import com.whistleup.backend.exception.BadRequestException;
import com.whistleup.backend.exception.NotFoundException;
import com.whistleup.backend.repository.BuildingDetailsRepository;
import com.whistleup.backend.repository.QrBuildingMappingRepository;
import com.whistleup.backend.resource.QrBuildingMappingResponse;
import com.whistleup.backend.resource.QrBuildingMappingUpsertRequest;
import com.whistleup.backend.service.QrBuildingMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QrBuildingMappingServiceImpl implements QrBuildingMappingService {

    private final QrBuildingMappingRepository qrBuildingMappingRepository;
    private final BuildingDetailsRepository buildingDetailsRepository;

    @Override
    @Transactional
    public QrBuildingMappingResponse upsert(QrBuildingMappingUpsertRequest request) {
        String token = normalizeToken(request.getToken());
        BuildingDetails building = buildingDetailsRepository
                .findById(request.getBuildingId())
                .orElseThrow(() -> new NotFoundException("Building not found: " + request.getBuildingId()));

        QrBuildingMapping mapping = qrBuildingMappingRepository
                .findByToken(token)
                .orElse(QrBuildingMapping.builder().token(token).build());

        mapping.setBuilding(building);
        QrBuildingMapping saved = qrBuildingMappingRepository.save(mapping);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public QrBuildingMappingResponse getByToken(String token) {
        String normalizedToken = normalizeToken(token);
        QrBuildingMapping mapping = qrBuildingMappingRepository
                .findByToken(normalizedToken)
                .orElseThrow(() -> new NotFoundException("QR mapping not found for token: " + normalizedToken));
        return toResponse(mapping);
    }

    private String normalizeToken(String token) {
        String value = token == null ? "" : token.trim();
        if (value.isEmpty()) {
            throw new BadRequestException("token is required");
        }
        if (value.length() > 200) {
            throw new BadRequestException("token is too long");
        }
        return value;
    }

    private QrBuildingMappingResponse toResponse(QrBuildingMapping mapping) {
        BuildingDetails building = mapping.getBuilding();
        return QrBuildingMappingResponse.builder()
                .token(mapping.getToken())
                .buildingId(building.getBuildingId())
                .buildingName(building.getBuildingName())
                .build();
    }
}
