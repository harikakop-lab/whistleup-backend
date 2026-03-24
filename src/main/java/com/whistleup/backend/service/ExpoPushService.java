package com.whistleup.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whistleup.backend.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpoPushService {

    static final String EXPO_PUSH_URL = "https://api.expo.dev/v2/push/send";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;
    private final DeviceTokenRepository deviceTokenRepository;

    public void send(String expoToken, String title, String body) {
        sendWithData(expoToken, title, body, Map.of());
    }

    public void sendWithData(
            String expoToken, String title, String body, Map<String, Object> data) {
        if (expoToken == null || expoToken.isBlank()) {
            return;
        }
        Map<String, Object> payload = basePayload(expoToken, title, body);
        payload.put("data", data != null ? data : Map.of());
        postAndHandleResponse(expoToken, payload);
    }

    private Map<String, Object> basePayload(String expoToken, String title, String body) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("to", expoToken);
        payload.put("title", title);
        payload.put("body", body);
        payload.put("sound", "default");
        payload.put("channelId", "default");
        payload.put("priority", "high");
        return payload;
    }

    private void postAndHandleResponse(String expoToken, Map<String, Object> payload) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            ResponseEntity<String> response = restTemplate.exchange(
                    EXPO_PUSH_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(payload, headers),
                    String.class);

            handleExpoResponse(expoToken, response.getBody());
        } catch (Exception ex) {
            log.error("Expo push HTTP error tokenPrefix={}", abbrev(expoToken), ex);
        }
    }

    private void handleExpoResponse(String expoToken, String body) {
        if (body == null || body.isBlank()) {
            log.warn("Expo push empty response body tokenPrefix={}", abbrev(expoToken));
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.get("data");
            if (data == null || data.isNull()) {
                log.warn("Expo push unexpected JSON (no data): {}", abbreviateJson(body));
                return;
            }
            if (data.isArray()) {
                for (JsonNode item : data) {
                    handleOneTicket(expoToken, item);
                }
            } else {
                handleOneTicket(expoToken, data);
            }
        } catch (Exception e) {
            log.warn("Expo push could not parse response tokenPrefix={} body={}",
                    abbrev(expoToken), abbreviateJson(body), e);
        }
    }

    private void handleOneTicket(String expoToken, JsonNode ticket) {
        String status = ticket.path("status").asText("");
        if (!"error".equals(status)) {
            return;
        }
        String err = ticket.path("details").path("error").asText("");
        String message = ticket.path("message").asText("");
        log.warn("Expo push ticket error: {} — {}", err, message);
        if ("DeviceNotRegistered".equals(err)) {
            deviceTokenRepository.deactivateByToken(expoToken);
        }
    }

    private static String abbrev(String token) {
        if (token == null) {
            return "null";
        }
        return token.length() <= 24 ? token : token.substring(0, 24) + "…";
    }

    private static String abbreviateJson(String body) {
        if (body.length() <= 200) {
            return body;
        }
        return body.substring(0, 200) + "…";
    }
}
