package com.whistleup.backend.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.whistleup.backend.constants.OrderStatus;
import com.whistleup.backend.constants.ServiceOrderType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceOrderResource {
    private UUID orderId;
    @NotNull
    private ServiceOrderType orderType;
    @NotEmpty
    private String profileId;
    @NotEmpty
    private String buildingId;
    @NotNull
    private LocalDate date;
    private LocalDate orderCreationDate;
    private UUID servicePersonId;
    @Builder.Default
    private OrderStatus orderStatus = OrderStatus.CREATED;
}
