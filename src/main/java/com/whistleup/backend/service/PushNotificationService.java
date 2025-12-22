package com.whistleup.backend.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class PushNotificationService {

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendPush(
            String pushToken,
            String title,
            String body,
            Map<String, Object> data
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("to", pushToken);
        payload.put("title", title);
        payload.put("body", body);
        payload.put("sound", "default");
        payload.put("data", data);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(payload, headers);

        restTemplate.postForEntity(
                "https://exp.host/--/api/v2/push/send",
                request,
                String.class
        );
    }
}
