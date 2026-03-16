package com.whistleup.backend.entity.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Converter
public class CustomExpensesConverter implements AttributeConverter<Map<String, BigDecimal>, String> {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, BigDecimal> attribute) {
        try {
            if (attribute == null || attribute.isEmpty()) {
                return null;
            }
            return mapper.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error converting custom expenses map", e);
        }
    }

    @Override
    public Map<String, BigDecimal> convertToEntityAttribute(String dbData) {
        try {
            if (dbData == null || dbData.isBlank()) {
                return new LinkedHashMap<>();
            }
            return mapper.readValue(dbData, new TypeReference<Map<String, BigDecimal>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Error reading custom expenses JSON", e);
        }
    }
}
