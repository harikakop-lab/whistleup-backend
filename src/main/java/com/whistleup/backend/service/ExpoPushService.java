package com.whistleup.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class ExpoPushService {

    private static final String EXPO_URL =
        "https://exp.host/--/api/v2/push/send";

    private final RestTemplate restTemplate = new RestTemplate();

    public void send(String expoToken, String title, String body) {

        Map<String, Object> payload = Map.of(
            "to", expoToken,
            "title", title,
            "body", body
        );

        restTemplate.postForEntity(EXPO_URL, payload, String.class);
    }
}