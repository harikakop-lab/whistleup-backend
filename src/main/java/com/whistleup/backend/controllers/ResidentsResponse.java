package com.whistleup.backend.controllers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResidentsResponse {

    private String phone;

    private String name;

    private String flatNo;

    public ResidentsResponse(String phone, String name, String flatNo) {
        this.phone = phone;
        this.name = name;
        this.flatNo = flatNo;
    }

}
