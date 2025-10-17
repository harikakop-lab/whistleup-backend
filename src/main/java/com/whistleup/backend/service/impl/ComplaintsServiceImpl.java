package com.whistleup.backend.service.impl;

import com.whistleup.backend.resource.ComplaintCreateResource;
import com.whistleup.backend.resource.ComplaintsResponseResource;
import com.whistleup.backend.service.ComplaintsService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ComplaintsServiceImpl implements ComplaintsService {
    @Override
    public List<ComplaintsResponseResource> getAllComplaints() {
        return List.of();
    }

    @Override
    public ComplaintsResponseResource getAllComplaintsById(String complaintId) {
        return null;
    }

    @Override
    public ComplaintsResponseResource getComplaintsByProfileId(String profileId, boolean isAssigned) {
        return null;
    }

    @Override
    public ComplaintsResponseResource registerComplaint(ComplaintCreateResource complaintCreateResource) {
        return null;
    }

    @Override
    public void deleteComplaint(String complaintId) {

    }
}
