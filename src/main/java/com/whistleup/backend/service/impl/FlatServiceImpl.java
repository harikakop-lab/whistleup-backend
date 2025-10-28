package com.whistleup.backend.service.impl;

import com.whistleup.backend.entity.FlatDetails;
import com.whistleup.backend.exception.NotFoundException;
import com.whistleup.backend.repository.FlatRepository;
import com.whistleup.backend.resource.FlatRequestResource;
import com.whistleup.backend.resource.FlatResponseResource;
import com.whistleup.backend.service.FlatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class FlatServiceImpl implements FlatService {

    private final FlatRepository flatRepository;

    public FlatServiceImpl(FlatRepository flatRepository) {
        this.flatRepository = flatRepository;
    }

    @Override
    public List<FlatResponseResource> getAllFlats() {
        List<FlatDetails> flatDetailsList = flatRepository.findAll();
        return flatDetailsList.stream().map(flatDetails -> {
            FlatResponseResource flatResponseResource = FlatResponseResource.builder().build();
            BeanUtils.copyProperties(flatDetails, flatResponseResource);
            return flatResponseResource;
        }).toList();
    }

    @Override
    public FlatResponseResource getFlatDetailsById(Long flatId) {
        FlatDetails flatDetails = flatRepository.findById(flatId).orElseThrow(() -> new NotFoundException("No Flat found with this given id: " + flatId));
        FlatResponseResource flatResponseResource = FlatResponseResource.builder().build();
        BeanUtils.copyProperties(flatDetails, flatResponseResource);
        return flatResponseResource;
    }

    @Override
    public FlatResponseResource addFlatDetails(FlatRequestResource flatRequestResource) {
        FlatDetails flatDetails = FlatDetails.builder().build();
        BeanUtils.copyProperties(flatRequestResource, flatDetails);
        FlatDetails savedFlatDetails = flatRepository.save(flatDetails);
        FlatResponseResource flatResponseResource = FlatResponseResource.builder().build();
        BeanUtils.copyProperties(savedFlatDetails, flatResponseResource);
        return flatResponseResource;
    }

    @Override
    public FlatResponseResource updateFlateDetails(Long flatId, FlatRequestResource flatRequestResource) {
        FlatDetails flatDetails = flatRepository.findById(flatId).orElseThrow(() -> new NotFoundException("No Flat found with this given id: " + flatId));
        FlatDetails flatEntity = FlatDetails.builder().build();
        BeanUtils.copyProperties(flatRequestResource, flatEntity);
        FlatDetails sabedFlatDetails = flatRepository.save(flatEntity);
        FlatResponseResource flatResponseResource = FlatResponseResource.builder().build();
        BeanUtils.copyProperties(sabedFlatDetails, flatResponseResource);
        return flatResponseResource;
    }

    @Override
    public void deleteFlatDetails(Long flatId) {
        flatRepository.findById(flatId).orElseThrow(() -> new NotFoundException("No Flat found with this given id: " + flatId));
        flatRepository.deleteById(flatId);
    }
}
