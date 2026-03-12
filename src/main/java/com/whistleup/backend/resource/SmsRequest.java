package com.whistleup.backend.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SmsRequest {

    @JsonProperty("Text")
    private String text;

    @JsonProperty("Number")
    private String number;

    @JsonProperty("SenderId")
    private String senderId;

    @JsonProperty("DRNotifyUrl")
    private String drNotifyUrl;

    @JsonProperty("DRNotifyHttpMethod")
    private String drNotifyHttpMethod;

    @JsonProperty("Tool")
    private String tool;

    // getters & setters
}
