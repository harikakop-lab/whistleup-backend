package com.whistleup.backend.service;

import com.whistleup.backend.constants.ServiceOrderType;
import com.whistleup.backend.entity.ServicePerson;
import com.whistleup.backend.exception.ServiceOrderNotFoundException;
import com.whistleup.backend.repository.ServicePersonRepository;
import com.whistleup.backend.resource.ServicePersonResource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServicePersonService {

    private final ServicePersonRepository servicePersonRepository;
    private final ServicePersonMapper servicePersonMapper;

    public List<ServicePersonResource> getAllServicePersons() {
        log.info("Fetching all service persons");
        return servicePersonRepository.findAll()
                .stream()
                .map(servicePersonMapper::toResource)
                .collect(Collectors.toList());
    }

    public List<ServicePersonResource> getActiveServicePersonsByType(ServiceOrderType serviceType) {
        log.info("Fetching active service persons for type: {}", serviceType);
        return servicePersonRepository.findActiveByServiceType(serviceType)
                .stream()
                .map(servicePersonMapper::toResource)
                .collect(Collectors.toList());
    }

    public ServicePersonResource getServicePersonById(String servicePersonId) {
        log.info("Fetching service person with id: {}", servicePersonId);
        return servicePersonRepository.findById(UUID.fromString(servicePersonId))
                .map(servicePersonMapper::toResource)
                .orElseThrow(() -> new ServiceOrderNotFoundException(
                        "Service person not found with id: " + servicePersonId));
    }

    public Optional<ServicePersonResource> findServicePersonByPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return Optional.empty();
        return servicePersonRepository.findByPhoneNumber(phoneNumber.trim())
                .map(servicePersonMapper::toResource);
    }

    @Transactional
    public ServicePersonResource createServicePerson(ServicePersonResource resource) {
        log.info("Creating service person: {}", resource.getName());
        if (servicePersonRepository.existsByPhoneNumber(resource.getPhoneNumber())) {
            throw new IllegalArgumentException(
                    "Service person already exists with phone number: " + resource.getPhoneNumber());
        }
        ServicePerson saved = servicePersonRepository.save(servicePersonMapper.toEntity(resource));
        return servicePersonMapper.toResource(saved);
    }

    @Transactional
    public ServicePersonResource updateServicePerson(String servicePersonId, ServicePersonResource resource) {
        log.info("Updating service person with id: {}", servicePersonId);
        ServicePerson existing = servicePersonRepository.findById(UUID.fromString(servicePersonId))
                .orElseThrow(() -> new ServiceOrderNotFoundException(
                        "Service person not found with id: " + servicePersonId));

        existing.setName(resource.getName());
        existing.setPhoneNumber(resource.getPhoneNumber());
        existing.setAddress(resource.getAddress());
        existing.setExperienceYears(resource.getExperienceYears());
        existing.setRating(resource.getRating());
        existing.setServiceTypes(resource.getServiceTypes());
        existing.setServiceCity(resource.getServiceCity());

        return servicePersonMapper.toResource(servicePersonRepository.save(existing));
    }

    @Transactional
    public void deactivateServicePerson(String servicePersonId) {
        log.info("Deactivating service person with id: {}", servicePersonId);
        ServicePerson existing = servicePersonRepository.findById(UUID.fromString(servicePersonId))
                .orElseThrow(() -> new ServiceOrderNotFoundException(
                        "Service person not found with id: " + servicePersonId));
        existing.setIsActive(false);
        servicePersonRepository.save(existing);
    }
}