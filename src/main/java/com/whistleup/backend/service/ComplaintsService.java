package com.whistleup.backend.service;

import com.whistleup.backend.resource.ComplaintCreateResource;
import com.whistleup.backend.resource.ComplaintsResponseResource;

import java.util.List;

public interface ComplaintsService {
    List<ComplaintsResponseResource> getAllComplaints();

    ComplaintsResponseResource getAllComplaintsById(String complaintId);

    ComplaintsResponseResource getComplaintsByProfileId(String profileId, boolean isAssigned);

    ComplaintsResponseResource registerComplaint(ComplaintCreateResource complaintCreateResource);

    void deleteComplaint(String complaintId);
}
