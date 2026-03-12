package com.whistleup.backend.service;

import com.whistleup.backend.constants.ComplaintStatus;
import com.whistleup.backend.resource.ComplaintCreateResource;
import com.whistleup.backend.resource.ComplaintImageResponse;
import com.whistleup.backend.resource.ComplaintsResponseResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ComplaintsService {
    List<ComplaintsResponseResource> getAllComplaints();

    ComplaintsResponseResource getAllComplaintsById(String complaintId);

    List<ComplaintsResponseResource> getComplaintsByProfileId(String profileId);

    List<ComplaintsResponseResource> getComplaintsByAssigneeProfileId(String profileId);

    ComplaintsResponseResource registerComplaint(ComplaintCreateResource complaintCreateResource, MultipartFile[] files) throws IOException;

    void deleteComplaint(String complaintId);

    void resolveTicket(String complaintId);

    ComplaintsResponseResource updateStatus(String complaintId, ComplaintStatus status);

//    List<ComplaintImageResponse> getImagesByComplaintId(String complaintId);
//
//    ComplaintImage getImage(Long imageId);
}
