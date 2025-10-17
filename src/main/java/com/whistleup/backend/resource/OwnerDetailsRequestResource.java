package com.whistleup.backend.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OwnerDetailsRequestResource {

    private String ownerName;
    private String flatDetails;
    private String ownerMobileNumber;
    private String ownerEmail;
    private AddressResource ownerAddress;

}
