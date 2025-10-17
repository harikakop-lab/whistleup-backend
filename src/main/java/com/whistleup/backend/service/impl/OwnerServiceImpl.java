package com.whistleup.backend.service.impl;

import com.whistleup.backend.entity.OwnerDetails;
import com.whistleup.backend.exception.NotFoundException;
import com.whistleup.backend.repository.OwnerRepository;
import com.whistleup.backend.resource.OwnerDetailsRequestResource;
import com.whistleup.backend.resource.OwnerDetailsResponseResource;
import com.whistleup.backend.service.OwnerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class OwnerServiceImpl implements OwnerService {

    private final OwnerRepository ownerRepository;

    public OwnerServiceImpl(OwnerRepository ownerRepository) {
        this.ownerRepository = ownerRepository;
    }

    @Override
    public List<OwnerDetailsResponseResource> getAllExistingOnwers() {
        List<OwnerDetails> ownerDetailsList = ownerRepository.findAll();
        return ownerDetailsList.stream().map(ownerDetails -> {
            OwnerDetailsResponseResource ownerDetailsResponseResource = new OwnerDetailsResponseResource();
            BeanUtils.copyProperties(ownerDetails, ownerDetailsResponseResource);
            return ownerDetailsResponseResource;
        }).toList();
    }

    @Override
    public OwnerDetailsResponseResource getOwnerDetailsById(Long ownerId) {
        OwnerDetails ownerDetailsEntity = ownerRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("No Owners found with this id"));
        OwnerDetailsResponseResource ownerDetailsResponseResource = OwnerDetailsResponseResource.builder().build();
        BeanUtils.copyProperties(ownerDetailsEntity, ownerDetailsResponseResource);
        return ownerDetailsResponseResource;
    }

    @Override
    public OwnerDetailsResponseResource saveOwnerDetails(OwnerDetailsRequestResource ownerDetailsRequestResource) {
        OwnerDetails ownerDetailsEntity = OwnerDetails.builder().build();
        BeanUtils.copyProperties(ownerDetailsRequestResource, ownerDetailsEntity);
        OwnerDetailsResponseResource ownerDetailsResponseResource = OwnerDetailsResponseResource.builder().build();
        BeanUtils.copyProperties(ownerDetailsEntity, ownerDetailsResponseResource);
        return ownerDetailsResponseResource;
    }

    @Override
    public OwnerDetailsResponseResource updateOwnerDetails(Long ownerId, OwnerDetailsRequestResource ownerDetailsRequestResource) {
        OwnerDetails existingOwnerDetailsEntity = ownerRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("No Owners found with this id"));
        existingOwnerDetailsEntity.setOwnerEmail(ownerDetailsRequestResource.getOwnerEmail());
        existingOwnerDetailsEntity.setOwnerName(ownerDetailsRequestResource.getOwnerName());
        existingOwnerDetailsEntity.setFlatDetails(ownerDetailsRequestResource.getFlatDetails());
        existingOwnerDetailsEntity.setOwnerMobileNumber(ownerDetailsRequestResource.getOwnerMobileNumber());
        existingOwnerDetailsEntity.setOwnerEmail(ownerDetailsRequestResource.getOwnerEmail());
        ownerRepository.save(existingOwnerDetailsEntity);
        OwnerDetailsResponseResource ownerDetailsResponseResource = OwnerDetailsResponseResource.builder().build();
        BeanUtils.copyProperties(existingOwnerDetailsEntity, ownerDetailsResponseResource);
        return ownerDetailsResponseResource;
    }

    @Override
    public String deleteOwnerDetails(Long ownerId) {
        OwnerDetails ownerDetailsEntity = ownerRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("No Owners found with this id"));

        ownerRepository.deleteById(ownerId);
        return "Deleted";
    }

}
