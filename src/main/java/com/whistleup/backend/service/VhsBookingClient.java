package com.whistleup.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VhsBookingClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${vhs.url}")
    private String vhsUrl;

    @Value("${vhs.auth}")
    private String vhsAuth;

    public String createBooking(
            String optionTitle,
            LocalDate date,
            String timeSlot,
            Integer amount,
            String customerName,
            String city,
            String address,
            String flatNo,
            String externalReference,
            String phone
    ) {
        String url = vhsUrl + "/create-booking";
        Map<String, Object> payload = new HashMap<>();
        payload.put("customerName", customerName);
        payload.put("service", optionTitle);
        payload.put("city", city);
        payload.put("date_of_service", date.format(DateTimeFormatter.ISO_DATE));
        payload.put("totalPayable", amount == null ? 0 : amount);
        payload.put("appointmentWindow", timeSlot);
        payload.put("serviceSummary", "1 x " + optionTitle);

        Map<String, String> serviceAddress = new HashMap<>();
        serviceAddress.put("address", address);
        serviceAddress.put("flatno", flatNo);
        payload.put("serviceAddress", serviceAddress);
        payload.put("phone", phone);
        payload.put("externalReference", externalReference);

        JsonNode response = callVhs(url, HttpMethod.POST, payload);
        String bookingId = getString(response, "internalBookingId");
        if (bookingId == null) bookingId = getString(response, "booking_id");
        if (bookingId == null) bookingId = getString(response, "id");
        if (bookingId == null) {
            throw new IllegalStateException("VHS create booking response missing booking id.");
        }
        return bookingId;
    }

    public JsonNode getBooking(String vhsBookingId) {
        String url = vhsUrl + "/bookings/" + vhsBookingId;
        return callVhs(url, HttpMethod.GET, null);
    }

    public void changeSlot(String vhsBookingId, String slotText) {
        String url = vhsUrl + "/bookings/" + vhsBookingId + "/change-slot";
        Map<String, Object> payload = Map.of("selected_slot_text", slotText);
        callVhs(url, HttpMethod.POST, payload);
    }

    public void changeDate(String vhsBookingId, String serviceDateIso) {
        String url = vhsUrl + "/bookings/" + vhsBookingId + "/change-date";
        Map<String, Object> payload = Map.of("date_of_service", serviceDateIso);
        callVhs(url, HttpMethod.POST, payload);
    }

    public void cancelBooking(String vhsBookingId, String reason) {
        String url = vhsUrl + "/bookings/" + vhsBookingId + "/cancel";
        Map<String, Object> payload = Map.of("cancelReason", reason);
        callVhs(url, HttpMethod.POST, payload);
    }

    private JsonNode callVhs(String url, HttpMethod method, Object body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", vhsAuth);
            if (body != null) {
                log.info("VHS request [{} {}]: {}", method, url, objectMapper.writeValueAsString(body));
            } else {
                log.info("VHS request [{} {}] with empty body", method, url);
            }
            HttpEntity<Object> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, method, request, String.class);
            String responseBody = response.getBody();
            if (responseBody == null || responseBody.isBlank()) {
                return objectMapper.createObjectNode();
            }
            return objectMapper.readTree(responseBody);
        } catch (HttpStatusCodeException ex) {
            String bodyText = ex.getResponseBodyAsString();
            log.error("VHS API failed: status={}, body={}", ex.getStatusCode(), bodyText);
            throw new IllegalStateException("VHS API error: " + ex.getStatusCode() + " " + bodyText);
        } catch (Exception ex) {
            log.error("VHS API call failed", ex);
            throw new IllegalStateException("Unable to connect with VHS service.");
        }
    }

    private String getString(JsonNode node, String field) {
        if (node == null || field == null) return null;
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) return null;
        return v.asText(null);
    }
}
