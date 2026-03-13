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
import com.whistleup.backend.service.ProfileService;
import com.whistleup.backend.util.CustomBeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
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

    public ProfileServiceImpl(ProfileRepository profileRepository, FlatRepository flatRepository, BuildingDetailsRepository buildingRepository, PasswordEncoder passwordEncoder) {
        this.profileRepository = profileRepository;
        this.flatRepository = flatRepository;
        this.buildingRepository = buildingRepository;
        this.passwordEncoder = passwordEncoder;
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
        try {
            Optional<Profile> profileOptional = profileRepository.findByPhone(userId);
            if (profileOptional.isEmpty()) {
                log.error("Profile not found with id: {}", userId);
                throw new NotFoundException("Profile not found");
            }
            profileRepository.deleteById(userId);
            return "SUCCESS";
        } catch (Exception e) {
            log.error("Error occurred while deleting the profile with id: {}", userId, e);
        }
        return "FAILURE";
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
        BeanUtils.copyProperties(profile, profileResponseResource);
        if (profile.getBuildingId() != null && !profile.getBuildingId().isBlank()) {
            try {
                Optional<BuildingDetails> buildingDetailsOptional = buildingRepository.findById(Long.valueOf(profile.getBuildingId()));
                buildingDetailsOptional.ifPresent(buildingDetails -> profileResponseResource.setBuildingName(buildingDetails.getBuildingName()));
            } catch (Exception ignore) {
                // Keep profile response even if building mapping is stale/invalid.
            }
        }
        return profileResponseResource;
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
        Optional<BuildingDetails> buildingOptional;
        try {
            buildingOptional = buildingRepository.findById(Long.valueOf(profile.getBuildingId()));
        } catch (Exception ex) {
            return;
        }
        if (buildingOptional.isEmpty()) {
            return;
        }
        FlatDetails flatDetails = flatRepository.findFlatByFlatNumber(profile.getFlatNo())
                .orElseGet(() -> FlatDetails.builder().build());
        flatDetails.setResident(profile);
        try {
            flatDetails.setFloor(profile.getFloor() == null || profile.getFloor().isBlank()
                    ? null
                    : Long.valueOf(profile.getFloor()));
        } catch (Exception ex) {
            flatDetails.setFloor(null);
        }
        flatDetails.setFlatNumber(profile.getFlatNo());
        flatDetails.setBuilding(buildingOptional.get());
        flatRepository.save(flatDetails);
    }

}
