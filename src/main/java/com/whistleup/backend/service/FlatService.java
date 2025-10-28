package com.whistleup.backend.service;

import com.whistleup.backend.resource.FlatRequestResource;
import com.whistleup.backend.resource.FlatResponseResource;

import java.util.List;

public interface FlatService {

    List<FlatResponseResource> getAllFlats();

    FlatResponseResource getFlatDetailsById(Long flatId);

    FlatResponseResource addFlatDetails(FlatRequestResource flatRequestResource);

    FlatResponseResource updateFlateDetails(Long flatId, FlatRequestResource flatRequestResource);

    void deleteFlatDetails(Long flatId);
}
