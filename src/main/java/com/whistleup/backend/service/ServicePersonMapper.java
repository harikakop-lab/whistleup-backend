package com.whistleup.backend.service;

import com.whistleup.backend.entity.ServicePerson;
import com.whistleup.backend.resource.ServicePersonResource;
import org.springframework.stereotype.Component;

@Component
public class ServicePersonMapper {

    public ServicePerson toEntity(ServicePersonResource resource) {
        return ServicePerson.builder()
                .name(resource.getName())
                .phoneNumber(resource.getPhoneNumber())
                .address(resource.getAddress())
                .experienceYears(resource.getExperienceYears())
                .rating(resource.getRating())
                .serviceTypes(resource.getServiceTypes())
                .build();
    }

    public ServicePersonResource toResource(ServicePerson entity) {
        return ServicePersonResource.builder()
                .servicePersonId(entity.getServicePersonId())
                .name(entity.getName())
                .phoneNumber(entity.getPhoneNumber())
                .address(entity.getAddress())
                .experienceYears(entity.getExperienceYears())
                .rating(entity.getRating())
                .serviceTypes(entity.getServiceTypes())
                .isActive(entity.getIsActive())
                .registeredDate(entity.getRegisteredDate())
                .build();
    }
}