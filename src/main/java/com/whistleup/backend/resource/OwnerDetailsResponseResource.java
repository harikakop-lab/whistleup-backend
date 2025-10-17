package com.whistleup.backend.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OwnerDetailsResponseResource {

    private Long ownerId;
    private String flatDetails;
    private String ownerMobileNumber;
    private String ownerEmail;
    private AddressResource ownerAddress;

}
