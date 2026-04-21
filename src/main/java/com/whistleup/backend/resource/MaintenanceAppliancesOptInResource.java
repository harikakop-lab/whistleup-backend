package com.whistleup.backend.resource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceAppliancesOptInResource {
    private String phone;
    private String name;
    private String flatNo;
    private String appliancesJson;
}
