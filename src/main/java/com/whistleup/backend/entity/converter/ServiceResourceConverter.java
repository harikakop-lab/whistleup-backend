package com.whistleup.backend.entity.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whistleup.backend.resource.ServiceResource;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ServiceResourceConverter implements AttributeConverter<ServiceResource, String> {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(ServiceResource attribute) {
        try {
            return attribute == null ? null : mapper.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error converting ServiceResource", e);
        }
    }

    @Override
    public ServiceResource convertToEntityAttribute(String dbData) {
        try {
            return dbData == null ? null : mapper.readValue(dbData, ServiceResource.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error reading ServiceResource JSON", e);
        }
    }
}
