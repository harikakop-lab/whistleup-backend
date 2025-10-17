package com.whistleup.backend.service;

import com.whistleup.backend.resource.OwnerDetailsRequestResource;
import com.whistleup.backend.resource.OwnerDetailsResponseResource;

import java.util.List;

public interface OwnerService {

    List<OwnerDetailsResponseResource> getAllExistingOnwers();
    OwnerDetailsResponseResource getOwnerDetailsById(Long ownerId);

    OwnerDetailsResponseResource saveOwnerDetails(OwnerDetailsRequestResource ownerDetailsRequestResource);

    OwnerDetailsResponseResource updateOwnerDetails(Long ownerId, OwnerDetailsRequestResource ownerDetailsRequestResource);

    String deleteOwnerDetails(Long ownerId);
}
