package com.whistleup.backend.service;

import com.whistleup.backend.resource.SmsRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class SmsCountryClient {

    private final RestClient restClient;

    public SmsCountryClient(RestClient smsCountryRestClient) {
        this.restClient = smsCountryRestClient;
    }

    public String sendSms(SmsRequest request) {
        return restClient.post()
                .uri("/Accounts/8nN8R11oDDKNJO7OCTKU/SMSes/")
                .body(request)
                .retrieve()
                .body(String.class);
    }
}
