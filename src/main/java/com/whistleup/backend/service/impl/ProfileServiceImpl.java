package com.whistleup.backend.service.impl;

import com.whistleup.backend.constants.AppConstants;
import com.whistleup.backend.constants.Roles;
import com.whistleup.backend.entity.BuildingDetails;
import com.whistleup.backend.entity.Contact;
import com.whistleup.backend.entity.FlatDetails;
import com.whistleup.backend.entity.Profile;
import com.whistleup.backend.exception.BadRequestException;
import com.whistleup.backend.exception.NotFoundException;
import com.whistleup.backend.repository.BuildingDetailsRepository;
import com.whistleup.backend.repository.FlatRepository;
import com.whistleup.backend.repository.ProfileRepository;
import com.whistleup.backend.resource.ContactResource;
import com.whistleup.backend.resource.ProfileCreateResource;
import com.whistleup.backend.resource.ProfileResponseResource;
import com.whistleup.backend.service.BuildingAdminService;
import com.whistleup.backend.service.FileStorageService;
import com.whistleup.backend.service.ProfileService;
import com.whistleup.backend.util.CustomBeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;

    private final FlatRepository flatRepository;

    private final BuildingDetailsRepository buildingRepository;

    private final PasswordEncoder passwordEncoder;

    private final BuildingAdminService buildingAdminService;

    private final FileStorageService fileStorageService;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    public ProfileServiceImpl(
            ProfileRepository profileRepository,
            FlatRepository flatRepository,
            BuildingDetailsRepository buildingRepository,
            PasswordEncoder passwordEncoder,
            BuildingAdminService buildingAdminService,
            FileStorageService fileStorageService) {
        this.profileRepository = profileRepository;
        this.flatRepository = flatRepository;
        this.buildingRepository = buildingRepository;
        this.passwordEncoder = passwordEncoder;
        this.buildingAdminService = buildingAdminService;
        this.fileStorageService = fileStorageService;
    }

    @Override
    public ProfileResponseResource createProfile(ProfileCreateResource profileCreateResource) {

        if (profileRepository.findByPhone(profileCreateResource.getPhone()).isPresent()) {
            throw new BadRequestException(
                    "Phone already exists",
                    "Try using a different phone number."
            );
        }

        Profile profile = Profile.builder().build();
        BeanUtils.copyProperties(profileCreateResource, profile, "contacts");
        if (profile.getRole() == null) {
            profile.setRole(Roles.USER);
        }
        if (profile.getPin() == null || profile.getPin().isBlank()) {
            throw new BadRequestException("PIN is required", "Please provide a valid 4-digit PIN.");
        }
        if (profile.getIsAssigned() == null) {
            // New user/owner self-signups wait for admin approval unless flat is already assigned.
            if (profile.getRole() == Roles.USER || profile.getRole() == Roles.OWNER) {
                boolean hasFlat = profile.getFlatNo() != null && !profile.getFlatNo().isBlank();
                log.info("Flat no: {}", profile.getFlatNo());
                log.info("hasFlat: {}", hasFlat);
                profile.setIsAssigned(hasFlat);
            } else {
                log.info("Is Assigned it true");
                profile.setIsAssigned(true);
            }
        }

        profile.setPin(passwordEncoder.encode(profile.getPin()));

        if (profileCreateResource.getContacts() != null) {
            validateEmergencyContactLimits(profileCreateResource.getContacts());
            profile.setContacts(new ArrayList<>());
            profileCreateResource.getContacts().forEach(c -> {
                Contact contact = Contact.builder()
                        .name(c.getName())
                        .phone(c.getPhone())
                        .contactKind(normalizeContactKind(c.getContactKind()))
                        .profile(profile)
                        .build();
                profile.getContacts().add(contact);
            });
        }

        Profile savedProfile = profileRepository.save(profile);
        upsertFlatDetails(savedProfile);
        ProfileResponseResource profileResponseResource = new ProfileResponseResource();
        profileResponseResource.setUserId(savedProfile.getPhone());
        return profileResponseResource;
    }

    @Override
    public String updateProfile(ProfileCreateResource profileUpdateResource) {
        Optional<Profile> profileOptional =
                profileRepository.findByPhone(profileUpdateResource.getPhone());
        if (profileOptional.isEmpty()) {
            log.error("Profile not found with phone: {}", profileUpdateResource.getPhone());
            throw new NotFoundException("Profile not found");
        }
        Profile profileEntity = profileOptional.get();
        // Copy non-null basic fields
        BeanUtils.copyProperties(
                profileUpdateResource,
                profileEntity,
                CustomBeanUtils.getNullPropertyNames(profileUpdateResource)
        );

        // Password update (only if provided)
        if (Objects.nonNull(profileUpdateResource.getPin())) {
            profileEntity.setPin(
                    passwordEncoder.encode(profileUpdateResource.getPin())
            );
        }

        if (profileUpdateResource.getContacts() != null
                && !profileUpdateResource.getContacts().isEmpty()) {

            if (profileEntity.getContacts() == null) {
                profileEntity.setContacts(new ArrayList<>());
            }

            validateEmergencyContactLimits(profileUpdateResource.getContacts());

            boolean hasEmergencyInRequest = profileUpdateResource.getContacts().stream()
                    .anyMatch(c -> AppConstants.CONTACT_KIND_EMERGENCY.equalsIgnoreCase(
                            String.valueOf(c.getContactKind() == null ? "" : c.getContactKind()).trim()));

            if (hasEmergencyInRequest) {
                profileEntity.getContacts()
                        .removeIf(ct -> AppConstants.CONTACT_KIND_EMERGENCY.equalsIgnoreCase(
                                String.valueOf(ct.getContactKind() == null ? "" : ct.getContactKind()).trim()));
            }

            Set<String> existingPhones = profileEntity.getContacts()
                    .stream()
                    .map(Contact::getPhone)
                    .collect(Collectors.toSet());

            for (var c : profileUpdateResource.getContacts()) {
                if (!existingPhones.contains(c.getPhone())) {
                    Contact contact = Contact.builder()
                            .name(c.getName())
                            .phone(c.getPhone())
                            .contactKind(normalizeContactKind(c.getContactKind()))
                            .profile(profileEntity)
                            .build();

                    profileEntity.getContacts().add(contact);
                    existingPhones.add(c.getPhone());
                }
            }
        }

        Profile updatedProfile = profileRepository.save(profileEntity);
        upsertFlatDetails(updatedProfile);
        return updatedProfile.getPhone();
    }


    @Override
    public String deleteProfile(String userId) {
        Optional<Profile> profileOptional = profileRepository.findByPhone(userId);
        if (profileOptional.isEmpty()) {
            log.error("Profile not found with id: {}", userId);
            throw new NotFoundException("Profile not found");
        }
        detachFlatMapping(userId);
        profileRepository.deleteById(userId);
        return "SUCCESS";
    }

    @Override
    public String deleteProfileAsRequester(String targetUserId, String requesterUsername) {
        Profile targetProfile = profileRepository.findByPhone(targetUserId)
                .orElseThrow(() -> new NotFoundException("Profile not found"));

        Profile requester = profileRepository.findByEmailOrPhone(requesterUsername)
                .orElseThrow(() -> new NotFoundException("Requester profile not found"));

        boolean isAdmin = requester.getRole() == Roles.ADMIN || requester.getRole() == Roles.SYSTEM_ADMIN;
        boolean isSelfDelete = Objects.equals(requester.getPhone(), targetProfile.getPhone())
                || (requester.getEmail() != null
                && targetProfile.getEmail() != null
                && requester.getEmail().equalsIgnoreCase(targetProfile.getEmail()));

        if (!isAdmin && !isSelfDelete) {
            throw new BadRequestException(
                    "Unauthorized delete request",
                    "Users may only delete their own account.");
        }

        detachFlatMapping(targetProfile.getPhone());
        profileRepository.deleteById(targetProfile.getPhone());
        return "SUCCESS";
    }

    @Override
    public ProfileResponseResource getProfileById(String userId) {
        Optional<Profile> profileOptional = profileRepository.findById(userId);
        if (profileOptional.isEmpty()) {
            throw new NotFoundException("No user found with userId: " + userId);
        }
        Profile profile = profileOptional.get();
        ProfileResponseResource profileResponseResource = new ProfileResponseResource();
        BeanUtils.copyProperties(profile, profileResponseResource, "contacts");
        profileResponseResource.setContacts(mapContactsToResources(profile.getContacts()));
        enrichAdminBuildings(profile, profileResponseResource);
        return profileResponseResource;
    }

    @Override
    public Roles getRole(String userName) {
        Optional<Profile> profileOptional = profileRepository.findByEmailOrPhone(userName);
        if (profileOptional.isEmpty()) {
            throw new NotFoundException("Role not found for the provided username");
        }
        return profileOptional.get().getRole();
    }

    @Override
    public ProfileResponseResource getProfileByUsername(String username) {
        Optional<Profile> profileOptional = profileRepository.findByEmailOrPhone(username);
        if (profileOptional.isEmpty()) {
            throw new NotFoundException("No user found with username: " + username);
        }
        Profile profile = profileOptional.get();
        ProfileResponseResource profileResponseResource = new ProfileResponseResource();
        BeanUtils.copyProperties(profile, profileResponseResource, "contacts");
        profileResponseResource.setContacts(mapContactsToResources(profile.getContacts()));
        if (profile.getBuildingId() != null && !profile.getBuildingId().isBlank()) {
            try {
                Optional<BuildingDetails> buildingDetailsOptional = buildingRepository.findById(Long.valueOf(profile.getBuildingId()));
                buildingDetailsOptional.ifPresent(buildingDetails -> profileResponseResource.setBuildingName(buildingDetails.getBuildingName()));
            } catch (Exception ignore) {
                // Keep profile response even if building mapping is stale/invalid.
            }
        }
        enrichAdminBuildings(profile, profileResponseResource);
        return profileResponseResource;
    }

    private void enrichAdminBuildings(Profile profile, ProfileResponseResource resource) {
        if (profile.getRole() != Roles.ADMIN && profile.getRole() != Roles.SYSTEM_ADMIN) {
            return;
        }
        String phone = profile.getPhone();
        if (phone == null || phone.isBlank()) {
            return;
        }
        resource.setAdminBuildings(
                buildingAdminService.resolveAdminBuildings(phone, profile.getBuildingId()));
    }

    @Override
    public void uploadProfileImage(String username, MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        if (Objects.nonNull(file.getContentType()) && !file.getContentType().startsWith("image/")) {
            throw new RuntimeException("Only image files are allowed");
        }

        Profile profile = profileRepository.findByEmailOrPhone(username)
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        String uploadDir = "uploads/profile-images/";
        Files.createDirectories(Paths.get(uploadDir));

        String filename = username + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(uploadDir, filename);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        profile.setProfileImagePath(filePath.toString());
        profileRepository.save(profile);
    }

    @Override
    public Resource getProfileImage(String username) throws MalformedURLException {
        Profile profile = profileRepository.findByEmailOrPhone(username)
                .orElseThrow(() -> new RuntimeException("Profile not found"));
        Path imagePath = Paths.get(profile.getProfileImagePath());
        return new UrlResource(imagePath.toUri());
    }

    @Override
    public boolean doesProfileExists(String username) {
        return profileRepository.findByEmailOrPhone(username).isPresent();
    }

    @Override
    public List<ContactResource> getContactsByUsername(String username) {
        Profile profile = profileRepository.findByEmailOrPhone(username).orElseThrow(() -> new RuntimeException("Profile not found"));
        return mapContactsToResources(profile.getContacts());
    }

    @Override
    public void uploadTenantDocument(String targetPhone, String kind, MultipartFile file, String requesterUsername) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File required", "Please attach a document.");
        }
        if (file.getSize() > AppConstants.MAX_TENANT_DOCUMENT_BYTES) {
            throw new BadRequestException("File too large", "Maximum upload size is 5 MB per file.");
        }
        Profile target = profileRepository.findByPhone(targetPhone)
                .orElseThrow(() -> new NotFoundException("Profile not found"));
        assertCanAccessTenantDocuments(requesterUsername, target);
        String storedName = fileStorageService.saveProfileTenantDocument(targetPhone, file);
        String k = normalizeDocKind(kind);
        switch (k) {
            case "ID_FRONT" -> target.setIdDocumentFrontPath(storedName);
            case "ID_BACK" -> target.setIdDocumentBackPath(storedName);
            case "COMPANY_ID" -> target.setCompanyIdDocumentPath(storedName);
            default -> throw new BadRequestException("Invalid document kind", "Use idFront, idBack, or companyId.");
        }
        profileRepository.save(target);
    }

    @Override
    public Resource getTenantDocument(String targetPhone, String kind, String requesterUsername) {
        Profile target = profileRepository.findByPhone(targetPhone)
                .orElseThrow(() -> new NotFoundException("Profile not found"));
        assertCanAccessTenantDocuments(requesterUsername, target);
        String k = normalizeDocKind(kind);
        String fileName = switch (k) {
            case "ID_FRONT" -> target.getIdDocumentFrontPath();
            case "ID_BACK" -> target.getIdDocumentBackPath();
            case "COMPANY_ID" -> target.getCompanyIdDocumentPath();
            default -> throw new BadRequestException("Invalid document kind", "Use idFront, idBack, or companyId.");
        };
        if (fileName == null || fileName.isBlank()) {
            throw new NotFoundException("Document not uploaded");
        }
        return fileStorageService.loadProfileTenantDocument(targetPhone, fileName);
    }

    @Override
    public ProfileResponseResource getResidentAdminDetail(String buildingId, String phone, String requesterUsername) {
        Profile target = profileRepository.findByPhone(phone)
                .orElseThrow(() -> new NotFoundException("Resident not found"));
        String requestedBuildingId = buildingId == null ? null : buildingId.trim();
        String targetBuildingId = target.getBuildingId() == null ? null : target.getBuildingId().trim();
        if (targetBuildingId == null || !targetBuildingId.equals(requestedBuildingId)) {
            throw new BadRequestException("Forbidden", "Resident is not in this building.");
        }
        // Auth checks intentionally bypassed: allow resident detail fetch without requester validation.
        ProfileResponseResource res = new ProfileResponseResource();
        BeanUtils.copyProperties(target, res, "contacts");
        res.setContacts(mapContactsToResources(target.getContacts()));
        res.setIdDocumentFrontUri(buildTenantDocUri(phone, "idFront", target.getIdDocumentFrontPath()));
        res.setIdDocumentBackUri(buildTenantDocUri(phone, "idBack", target.getIdDocumentBackPath()));
        res.setCompanyIdDocumentUri(buildTenantDocUri(phone, "companyId", target.getCompanyIdDocumentPath()));
        return res;
    }

    private String buildTenantDocUri(String phone, String kind, String storedFileName) {
        if (storedFileName == null || storedFileName.isBlank()) {
            return null;
        }
        String base = appBaseUrl.endsWith("/") ? appBaseUrl.substring(0, appBaseUrl.length() - 1) : appBaseUrl;
        return base + "/whistleup/profile/" + phone + "/tenant-document/" + kind;
    }

    private void assertCanAccessTenantDocuments(String requesterUsername, Profile target) {
        // Auth checks intentionally bypassed for tenant document flows in current setup.
        return;
    }

    private static String normalizeDocKind(String kind) {
        if (kind == null) {
            return "";
        }
        return switch (kind.trim().toLowerCase(Locale.ROOT)) {
            case "idfront", "id_front" -> "ID_FRONT";
            case "idback", "id_back" -> "ID_BACK";
            case "companyid", "company_id" -> "COMPANY_ID";
            default -> kind.trim().toUpperCase(Locale.ROOT);
        };
    }

    private static String normalizeContactKind(String raw) {
        if (raw == null || raw.isBlank()) {
            return AppConstants.CONTACT_KIND_GENERAL;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if (AppConstants.CONTACT_KIND_EMERGENCY.equals(u)) {
            return AppConstants.CONTACT_KIND_EMERGENCY;
        }
        return AppConstants.CONTACT_KIND_GENERAL;
    }

    private static void validateEmergencyContactLimits(List<ContactResource> contacts) {
        if (contacts == null) {
            return;
        }
        long emergency = contacts.stream()
                .filter(c -> AppConstants.CONTACT_KIND_EMERGENCY.equalsIgnoreCase(
                        String.valueOf(c.getContactKind() == null ? "" : c.getContactKind()).trim()))
                .count();
        if (emergency > AppConstants.MAX_EMERGENCY_CONTACTS) {
            throw new BadRequestException(
                    "Too many emergency contacts",
                    "You can add at most " + AppConstants.MAX_EMERGENCY_CONTACTS + " emergency contacts.");
        }
    }

    private static List<ContactResource> mapContactsToResources(List<Contact> contacts) {
        if (contacts == null || contacts.isEmpty()) {
            return List.of();
        }
        return contacts.stream().map(contact -> {
            ContactResource resource = new ContactResource();
            resource.setName(contact.getName());
            resource.setPhone(contact.getPhone());
            resource.setContactKind(
                    contact.getContactKind() == null || contact.getContactKind().isBlank()
                            ? AppConstants.CONTACT_KIND_GENERAL
                            : contact.getContactKind());
            return resource;
        }).toList();
    }

    private void upsertFlatDetails(Profile profile) {
        if (profile.getBuildingId() == null || profile.getBuildingId().trim().isEmpty()) {
            return;
        }
        if (profile.getFlatNo() == null || profile.getFlatNo().trim().isEmpty()) {
            return;
        }
        Optional<BuildingDetails> buildingOptional;
        try {
            buildingOptional = buildingRepository.findById(Long.valueOf(profile.getBuildingId()));
        } catch (Exception ex) {
            return;
        }
        if (buildingOptional.isEmpty()) {
            return;
        }

        Long buildingId = buildingOptional.get().getBuildingId();
        String flatNo = profile.getFlatNo().trim();

        Optional<FlatDetails> residentCurrentFlatOptional = flatRepository.findByResident_Phone(profile.getPhone());
        Optional<FlatDetails> targetFlatOptional =
                flatRepository.findByBuilding_BuildingIdAndFlatNumber(buildingId, flatNo);

        if (residentCurrentFlatOptional.isPresent()) {
            FlatDetails residentCurrentFlat = residentCurrentFlatOptional.get();
            boolean movingToDifferentFlat = targetFlatOptional
                    .map(targetFlat -> !Objects.equals(targetFlat.getFlatId(), residentCurrentFlat.getFlatId()))
                    .orElse(true);
            if (movingToDifferentFlat) {
                residentCurrentFlat.setResident(null);
                flatRepository.save(residentCurrentFlat);
            }
        }

        FlatDetails flatDetails = targetFlatOptional.orElseGet(FlatDetails::new);
        Profile existingResident = flatDetails.getResident();
        if (existingResident != null && !Objects.equals(existingResident.getPhone(), profile.getPhone())) {
            throw new BadRequestException(
                    "Flat is already assigned",
                    "Selected flat is assigned to another resident."
            );
        }

        flatDetails.setResident(profile);
        try {
            flatDetails.setFloor(profile.getFloor() == null || profile.getFloor().isBlank()
                    ? null
                    : Long.valueOf(profile.getFloor()));
        } catch (Exception ex) {
            flatDetails.setFloor(null);
        }
        flatDetails.setFlatNumber(flatNo);
        flatDetails.setBuilding(buildingOptional.get());
        flatRepository.save(flatDetails);
    }

    private void detachFlatMapping(String profilePhone) {
        if (profilePhone == null || profilePhone.trim().isEmpty()) {
            return;
        }
        flatRepository.findByResident_Phone(profilePhone.trim()).ifPresent(flatDetails -> {
            flatDetails.setBuilding(null);
            flatDetails.setResident(null);
            flatRepository.save(flatDetails);
        });
    }

}
