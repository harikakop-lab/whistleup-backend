package com.whistleup.backend.service.impl;

import com.whistleup.backend.entity.Complaints;
import com.whistleup.backend.exception.NotFoundException;
import com.whistleup.backend.repository.ComplaintsRepository;
import com.whistleup.backend.resource.ComplaintCreateResource;
import com.whistleup.backend.resource.ComplaintsResponseResource;
import com.whistleup.backend.service.ComplaintsService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ComplaintsServiceImpl implements ComplaintsService {

    private final ComplaintsRepository complaintsRepository;

    public ComplaintsServiceImpl(ComplaintsRepository complaintsRepository) {
        this.complaintsRepository = complaintsRepository;
    }

    @Override
    public List<ComplaintsResponseResource> getAllComplaints() {
        List<Complaints> complaints = complaintsRepository.findAll();
        return complaints.stream().map(complaintEntity -> {
            ComplaintsResponseResource complaintsResponseResource = new ComplaintsResponseResource();
            BeanUtils.copyProperties(complaintEntity, complaintsResponseResource);
            return complaintsResponseResource;
        }).toList();
    }

    @Override
    public ComplaintsResponseResource getAllComplaintsById(String complaintId) {
        Complaints complaintEntity = complaintsRepository.findById(Long.valueOf(complaintId)).orElseThrow(() -> new NotFoundException("No Complaints/suggestions found with this given id: " + complaintId));
        ComplaintsResponseResource complaintsResponseResource = ComplaintsResponseResource.builder().build();
        BeanUtils.copyProperties(complaintEntity, complaintsResponseResource);
        return complaintsResponseResource;
    }

    @Override
    public List<ComplaintsResponseResource> getComplaintsByProfileId(String profileId) {
        List<Complaints> complaints = complaintsRepository.findByProfileId(profileId).orElseThrow(() -> new NotFoundException("No Complaints/suggestions found for this given profile id: " + profileId));
        return complaints.stream().map(complaint -> {
            ComplaintsResponseResource complaintsResponseResource = ComplaintsResponseResource.builder().build();
            BeanUtils.copyProperties(complaint, complaintsResponseResource);
            return complaintsResponseResource;
        }).toList();
    }

    @Override
    public ComplaintsResponseResource registerComplaint(ComplaintCreateResource complaintCreateResource) {
        Complaints complaintEntity = Complaints.builder().build();
        BeanUtils.copyProperties(complaintCreateResource, complaintEntity);
        complaintEntity.setProfileId(complaintCreateResource.getUsername());
        Complaints savedEntity = complaintsRepository.save(complaintEntity);
        ComplaintsResponseResource complaintsResponseResource = ComplaintsResponseResource.builder().build();
        BeanUtils.copyProperties(savedEntity, complaintsResponseResource);
        return complaintsResponseResource;
    }

    @Override
    public void deleteComplaint(String complaintId) {
        complaintsRepository.deleteById(Long.valueOf(complaintId));
    }
}
