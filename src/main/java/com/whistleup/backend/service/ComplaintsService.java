package com.whistleup.backend.service;

import com.whistleup.backend.resource.ComplaintCreateResource;
import com.whistleup.backend.resource.ComplaintImageResponse;
import com.whistleup.backend.resource.ComplaintsResponseResource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ComplaintsService {
    List<ComplaintsResponseResource> getAllComplaints();

    ComplaintsResponseResource getAllComplaintsById(String complaintId);

    List<ComplaintsResponseResource> getComplaintsByProfileId(String profileId);

    List<ComplaintsResponseResource> getComplaintsByAssigneeProfileId(String profileId);

    ComplaintsResponseResource registerComplaint(ComplaintCreateResource complaintCreateResource, MultipartFile[] files);

    void deleteComplaint(String complaintId);

//    List<ComplaintImageResponse> getImagesByComplaintId(String complaintId);
//
//    ComplaintImage getImage(Long imageId);
}
