package com.whistleup.backend.resource;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class AddressResource {

    private String street;
    @NotEmpty
    private String city;
    @NotEmpty
    private String state;
    @NotEmpty
    private String pincode;
}
