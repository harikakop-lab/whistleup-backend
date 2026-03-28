package com.whistleup.backend.resource;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class VhsWebhookUpdateRequest {

    /** VHS internal booking id (same value returned when creating a booking). */
    @JsonAlias({
        "internalBookingId",
        "internal_booking_id",
        "booking_id",
        "bookingId",
        "id"
    })
    private String vhsBookingId;

    @JsonAlias({"booking_status", "bookingStatus", "vhs_status", "state"})
    private String status;

    @JsonAlias({
        "assignedTechnicianName",
        "technician_name",
        "servicePersonName",
        "technicianName"
    })
    private String servicePersonName;

    @JsonAlias({
        "assignedTechnicianPhone",
        "technician_phone",
        "servicePersonPhone",
        "technicianPhone"
    })
    private String servicePersonPhone;
}
