package com.whistleup.backend.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.whistleup.backend.constants.OrderStatus;
import com.whistleup.backend.constants.ServiceIssueStatus;
import com.whistleup.backend.constants.ServiceOrderType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceOrderResource {
    private Long orderId;
    private ServiceOrderType orderType;
    private String profileId;
    private String buildingId;
    private String serviceCity;
    private LocalDate date;
    private String timeSlot;
    private String optionId;
    private String optionTitle;
    private String notes;
    private Integer amount;
    private String vhsBookingId;
    private String vhsStatus;
    private String vhsServicePersonName;
    private String vhsServicePersonPhone;
    private LocalDate orderCreationDate;
    private UUID servicePersonId;
    private String servicePersonName;
    private String servicePersonPhone;
    private String servicePersonRating;
    @Builder.Default
    private OrderStatus orderStatus = OrderStatus.CREATED;
    private ServiceIssueStatus issueStatus;
    private String issueText;
    private LocalDateTime issueRaisedAt;
}
