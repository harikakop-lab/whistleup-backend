package com.whistleup.backend.service.impl;

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
import com.whistleup.backend.service.ProfileService;
import com.whistleup.backend.util.CustomBeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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

    public ProfileServiceImpl(
            ProfileRepository profileRepository,
            FlatRepository flatRepository,
            BuildingDetailsRepository buildingRepository,
            PasswordEncoder passwordEncoder,
            BuildingAdminService buildingAdminService) {
        this.profileRepository = profileRepository;
        this.flatRepository = flatRepository;
        this.buildingRepository = buildingRepository;
        this.passwordEncoder = passwordEncoder;
        this.buildingAdminService = buildingAdminService;
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
        BeanUtils.copyProperties(profileCreateResource, profile);
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
            profile.setContacts(new ArrayList<>());
            profileCreateResource.getContacts().forEach(c -> {
                Contact contact = Contact.builder()
                        .name(c.getName())
                        .phone(c.getPhone())
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
    @CacheEvict(value = "profiles", key = "#username")
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

            // Optional: prevent duplicate contacts by phone
            Set<String> existingPhones = profileEntity.getContacts()
                    .stream()
                    .map(Contact::getPhone)
                    .collect(Collectors.toSet());

            for (var c : profileUpdateResource.getContacts()) {
                if (!existingPhones.contains(c.getPhone())) {
                    Contact contact = Contact.builder()
                            .name(c.getName())
                            .phone(c.getPhone())
                            .profile(profileEntity) // IMPORTANT
                            .build();

                    profileEntity.getContacts().add(contact);
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
        BeanUtils.copyProperties(profile, profileResponseResource);
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
    @Cacheable(value = "profiles", key = "#username", unless = "#result == null")
    public ProfileResponseResource getProfileByUsername(String username) {
        Optional<Profile> profileOptional = profileRepository.findByEmailOrPhone(username);
        if (profileOptional.isEmpty()) {
            throw new NotFoundException("No user found with username: " + username);
        }
        Profile profile = profileOptional.get();
        ProfileResponseResource profileResponseResource = new ProfileResponseResource();
        BeanUtils.copyProperties(profile, profileResponseResource);
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
        List<Contact> contacts = profile.getContacts();
        return contacts.stream().map(contact -> {
            ContactResource resource = new ContactResource();
            BeanUtils.copyProperties(contact, resource);
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
