package com.whistleup.backend.service.admin;

import com.whistleup.backend.constants.ComplaintStatus;
import com.whistleup.backend.constants.IssueType;
import com.whistleup.backend.entity.BuildingDetails;
import com.whistleup.backend.entity.ComplaintImage;
import com.whistleup.backend.entity.Complaints;
import com.whistleup.backend.entity.Contact;
import com.whistleup.backend.entity.Profile;
import com.whistleup.backend.entity.ServiceOrder;
import com.whistleup.backend.exception.NotFoundException;
import com.whistleup.backend.mapper.ServiceOrderMapper;
import com.whistleup.backend.repository.BuildingDetailsRepository;
import com.whistleup.backend.repository.ComplaintImageRepository;
import com.whistleup.backend.repository.ComplaintsRepository;
import com.whistleup.backend.repository.ProfileRepository;
import com.whistleup.backend.repository.ServiceOrderRepository;
import com.whistleup.backend.resource.BuildingDetailsRequestResource;
import com.whistleup.backend.resource.BuildingDetailsResponseResource;
import com.whistleup.backend.resource.ComplaintsResponseResource;
import com.whistleup.backend.resource.ContactResource;
import com.whistleup.backend.resource.ServiceOrderResource;
import com.whistleup.backend.resource.admin.AdminBuildingNotificationRequest;
import com.whistleup.backend.resource.admin.AdminProfileResponse;
import com.whistleup.backend.resource.admin.AdminProfileUpdateRequest;
import com.whistleup.backend.service.BuildingDetailsService;
import com.whistleup.backend.service.ComplaintsService;
import com.whistleup.backend.service.NotificationSendService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminPortalService {

    private final ServiceOrderRepository serviceOrderRepository;
    private final ServiceOrderMapper serviceOrderMapper;
    private final ComplaintsRepository complaintsRepository;
    private final ComplaintsService complaintsService;
    private final ComplaintImageRepository complaintImageRepository;
    private final ProfileRepository profileRepository;
    private final BuildingDetailsRepository buildingDetailsRepository;
    private final BuildingDetailsService buildingDetailsService;
    private final NotificationSendService notificationSendService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public Page<ServiceOrderResource> getServiceOrders(String q, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "orderId"));
        Specification<ServiceOrder> specification = containsText(
                normalizeQuery(q),
                root -> List.of(
                        root.get("profileId").as(String.class),
                        root.get("buildingId").as(String.class),
                        root.get("timeSlot").as(String.class),
                        root.get("optionId").as(String.class),
                        root.get("optionTitle").as(String.class),
                        root.get("notes").as(String.class),
                        root.get("vhsBookingId").as(String.class),
                        root.get("vhsStatus").as(String.class),
                        root.get("vhsServicePersonName").as(String.class),
                        root.get("vhsServicePersonPhone").as(String.class),
                        root.get("orderId").as(String.class),
                        root.get("amount").as(String.class),
                        root.get("orderStatus").as(String.class),
                        root.get("issueStatus").as(String.class)
                )
        );
        return serviceOrderRepository.findAll(specification, pageable).map(serviceOrderMapper::toResource);
    }

    public ServiceOrderResource getServiceOrder(Long orderId) {
        ServiceOrder order = serviceOrderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("No service order found with id: " + orderId));
        return serviceOrderMapper.toResource(order);
    }

    public Page<ComplaintsResponseResource> getComplaints(String q, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "complaintId"));
        Specification<Complaints> specification = containsText(
                normalizeQuery(q),
                root -> List.of(
                        root.get("profileId").as(String.class),
                        root.get("username").as(String.class),
                        root.get("type").as(String.class),
                        root.get("title").as(String.class),
                        root.get("description").as(String.class),
                        root.get("assigneeProfile").as(String.class),
                        root.get("buildingId").as(String.class),
                        root.get("status").as(String.class),
                        root.get("complaintId").as(String.class)
                )
        );
        return complaintsRepository.findAll(specification, pageable).map(this::toComplaintResponse);
    }

    public ComplaintsResponseResource getComplaint(Long complaintId) {
        Complaints complaint = complaintsRepository.findById(complaintId)
                .orElseThrow(() -> new NotFoundException("No complaint found with id: " + complaintId));
        return toComplaintResponse(complaint);
    }

    public ComplaintsResponseResource updateComplaintStatus(Long complaintId, String status) {
        String normalizedStatus = status == null ? "" : status.trim().toUpperCase(Locale.ENGLISH);
        if (normalizedStatus.isEmpty()) {
            throw new IllegalArgumentException("status is required");
        }
        return complaintsService.updateStatus(String.valueOf(complaintId), ComplaintStatus.valueOf(normalizedStatus));
    }

    public ComplaintImage getComplaintImage(Long imageId) {
        return complaintImageRepository.findById(imageId)
                .orElseThrow(() -> new NotFoundException("Image not found with id: " + imageId));
    }

    @Transactional
    public int sendNotificationToBuilding(AdminBuildingNotificationRequest request) {
        List<Profile> profiles = profileRepository.findByBuildingId(request.getBuildingId());
        Set<String> uniquePhones = profiles.stream()
                .map(Profile::getPhone)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());

        String type = StringUtils.hasText(request.getType()) ? request.getType() : IssueType.INFO.name();
        for (String phone : uniquePhones) {
            notificationSendService.notifyUserByProfilePhone(phone, request.getTitle(), request.getDescription(), type);
        }
        return uniquePhones.size();
    }

    public Page<AdminProfileResponse> getProfiles(String q, String buildingId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Specification<Profile> specification = containsText(
                normalizeQuery(q),
                root -> List.of(
                        root.get("phone").as(String.class),
                        root.get("name").as(String.class),
                        root.get("email").as(String.class),
                        root.get("buildingId").as(String.class),
                        root.get("floor").as(String.class),
                        root.get("flatNo").as(String.class),
                        root.get("upiId").as(String.class),
                        root.get("role").as(String.class),
                        root.get("isAssigned").as(String.class)
                )
        );
        if (StringUtils.hasText(buildingId)) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("buildingId"), buildingId.trim()));
        }
        return profileRepository.findAll(specification, pageable).map(this::toAdminProfileResponse);
    }

    public AdminProfileResponse getProfile(String phone) {
        Profile profile = profileRepository.findById(phone)
                .orElseThrow(() -> new NotFoundException("No profile found with phone: " + phone));
        return toAdminProfileResponse(profile);
    }

    @Transactional
    public AdminProfileResponse updateProfile(String phone, AdminProfileUpdateRequest request) {
        Profile profile = profileRepository.findById(phone)
                .orElseThrow(() -> new NotFoundException("No profile found with phone: " + phone));

        if (StringUtils.hasText(request.getName())) {
            profile.setName(request.getName().trim());
        }
        if (request.getEmail() != null) {
            profile.setEmail(request.getEmail().trim());
        }
        if (request.getRole() != null) {
            profile.setRole(request.getRole());
        }
        if (request.getUpiId() != null) {
            profile.setUpiId(request.getUpiId().trim());
        }
        if (request.getBuildingId() != null) {
            profile.setBuildingId(request.getBuildingId().trim());
        }
        if (request.getFloor() != null) {
            profile.setFloor(request.getFloor().trim());
        }
        if (request.getFlatNo() != null) {
            profile.setFlatNo(request.getFlatNo().trim());
        }
        if (request.getIsAssigned() != null) {
            profile.setIsAssigned(request.getIsAssigned());
        }
        if (StringUtils.hasText(request.getPin())) {
            profile.setPin(passwordEncoder.encode(request.getPin().trim()));
        }
        if (request.getContacts() != null) {
            List<Contact> contacts = new ArrayList<>();
            for (ContactResource contactResource : request.getContacts()) {
                if (contactResource == null || !StringUtils.hasText(contactResource.getPhone())) {
                    continue;
                }
                contacts.add(Contact.builder()
                        .name(contactResource.getName())
                        .phone(contactResource.getPhone())
                        .profile(profile)
                        .build());
            }
            profile.setContacts(contacts);
        }

        return toAdminProfileResponse(profileRepository.save(profile));
    }

    public Page<BuildingDetailsResponseResource> getBuildings(String q, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "buildingName"));
        Specification<BuildingDetails> specification = containsText(
                normalizeQuery(q),
                root -> List.of(
                        root.get("buildingName").as(String.class),
                        root.get("profileId").as(String.class),
                        root.get("adminName").as(String.class),
                        root.get("adminPhone").as(String.class),
                        root.get("adminEmail").as(String.class),
                        root.get("upiId").as(String.class),
                        root.get("buildingId").as(String.class),
                        root.get("buildingAddress").get("city").as(String.class),
                        root.get("buildingAddress").get("state").as(String.class),
                        root.get("buildingAddress").get("pincode").as(String.class),
                        root.get("buildingAddress").get("fullAddress").as(String.class),
                        root.get("buildingAddress").get("landmark").as(String.class),
                        root.get("buildingAddress").get("streetName").as(String.class)
                )
        );
        return buildingDetailsRepository.findAll(specification, pageable).map(this::toBuildingResponse);
    }

    public BuildingDetailsResponseResource getBuilding(Long buildingId) {
        BuildingDetails buildingDetails = buildingDetailsService.getBuildingDetails(buildingId);
        return toBuildingResponse(buildingDetails);
    }

    public BuildingDetailsResponseResource updateBuilding(Long buildingId, BuildingDetailsRequestResource request) {
        BuildingDetails buildingDetails = buildingDetailsService.updateBuildingDetails(buildingId, request);
        return toBuildingResponse(buildingDetails);
    }

    private BuildingDetailsResponseResource toBuildingResponse(BuildingDetails buildingDetails) {
        BuildingDetailsResponseResource response = new BuildingDetailsResponseResource();
        BeanUtils.copyProperties(buildingDetails, response);
        response.setBuildingId(buildingDetails.getBuildingId());
        response.setBuildingName(buildingDetails.getBuildingName());
        response.setBuildingAddress(buildingDetails.getBuildingAddress());
        response.setFloors(buildingDetails.getFloors());
        response.setTotalFlats(buildingDetails.getTotalFlats());
        response.setFlatStartNumber(buildingDetails.getFlatStartNumber());
        response.setFlatEndNumber(buildingDetails.getFlatEndNumber());
        response.setTotalResidents(buildingDetails.getTotalResidents());
        response.setAdminName(buildingDetails.getAdminName());
        response.setAdminPhone(buildingDetails.getAdminPhone());
        response.setAdminEmail(buildingDetails.getAdminEmail());
        response.setWaterBillRequired(buildingDetails.isWaterBillRequired());
        response.setUpiId(buildingDetails.getUpiId());
        return response;
    }

    private ComplaintsResponseResource toComplaintResponse(Complaints complaint) {
        ComplaintsResponseResource response = ComplaintsResponseResource.builder().build();
        BeanUtils.copyProperties(complaint, response);
        Optional<Profile> profileOptional = profileRepository.findByPhone(complaint.getProfileId());
        response.setRaisedBy(profileOptional.map(Profile::getName).orElse(""));
        response.setFlatNumber(profileOptional.map(Profile::getFlatNo).orElse(""));
        response.setStatus(Objects.requireNonNullElse(
                complaint.getStatus(),
                complaint.isResolved() ? ComplaintStatus.RESOLVED : ComplaintStatus.OPEN
        ));
        response.setCreatedAt(complaint.getCreatedAt() != null ? complaint.getCreatedAt().toString() : null);
        response.setUpdatedAt(complaint.getUpdatedAt() != null ? complaint.getUpdatedAt().toString() : null);
        response.setImageUrls(buildImageUrls(complaint));
        return response;
    }

    private List<String> buildImageUrls(Complaints complaint) {
        if (CollectionUtils.isEmpty(complaint.getImages())) {
            return List.of();
        }
        return complaint.getImages()
                .stream()
                .map(img -> baseUrl + "/whistleup/issues/images/" + img.getId())
                .toList();
    }

    private AdminProfileResponse toAdminProfileResponse(Profile profile) {
        List<ContactResource> contacts = profile.getContacts() == null
                ? List.of()
                : profile.getContacts().stream()
                .map(contact -> new ContactResource(contact.getName(), contact.getPhone()))
                .toList();

        return AdminProfileResponse.builder()
                .phone(profile.getPhone())
                .name(profile.getName())
                .email(profile.getEmail())
                .role(profile.getRole())
                .upiId(profile.getUpiId())
                .buildingId(profile.getBuildingId())
                .floor(profile.getFloor())
                .flatNo(profile.getFlatNo())
                .isAssigned(profile.getIsAssigned())
                .contacts(contacts)
                .build();
    }

    private static String normalizeQuery(String q) {
        return q == null ? "" : q.trim().toLowerCase(Locale.ENGLISH);
    }

    private static <T> Specification<T> containsText(
            String query,
            Function<jakarta.persistence.criteria.Root<T>, List<jakarta.persistence.criteria.Expression<String>>> expressionSupplier) {
        if (!StringUtils.hasText(query)) {
            return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.conjunction();
        }
        return (root, criteriaQuery, criteriaBuilder) -> {
            String pattern = "%" + query + "%";
            List<jakarta.persistence.criteria.Predicate> predicates = expressionSupplier.apply(root)
                    .stream()
                    .map(expression -> criteriaBuilder.like(criteriaBuilder.lower(expression), pattern))
                    .collect(Collectors.toCollection(ArrayList::new));
            return criteriaBuilder.or(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}
