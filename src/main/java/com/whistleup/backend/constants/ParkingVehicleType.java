package com.whistleup.backend.constants;

import com.whistleup.backend.exception.BadRequestException;

public enum ParkingVehicleType {
    TWO_WHEELER,
    FOUR_WHEELER;

    public static ParkingVehicleType fromRaw(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("vehicleType is required");
        }
        String normalized = raw.trim().toUpperCase()
                .replace("-", "_")
                .replace(" ", "_");
        if ("2_WHEELER".equals(normalized)) {
            normalized = "TWO_WHEELER";
        } else if ("4_WHEELER".equals(normalized)) {
            normalized = "FOUR_WHEELER";
        }
        try {
            return ParkingVehicleType.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Invalid vehicleType: " + raw);
        }
    }
}
