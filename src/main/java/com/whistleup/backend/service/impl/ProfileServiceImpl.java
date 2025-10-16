package com.whistleup.backend.service.impl;

import com.whistleup.backend.exception.BadRequestException;
import com.whistleup.backend.exception.NotFoundException;
import com.whistleup.backend.resource.ProfileCreateResource;
import com.whistleup.backend.resource.ProfileResponseResource;
import com.whistleup.backend.entity.Profile;
import com.whistleup.backend.service.ProfileService;
import com.whistleup.backend.util.CustomBeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Service;

import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.Optional;

@Service
@Slf4j
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;

    public ProfileServiceImpl(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Override
    public ProfileResponseResource createProfile(ProfileCreateResource profileCreateResource) {
        if (profileRepository.findByEmail(profileCreateResource.getEmail()).isPresent()) {
            throw new BadRequestException("Email already exists", "Try using a different email address.");
        }
        Profile profile = Profile.builder().build();
        BeanUtils.copyProperties(profileCreateResource, profile);
        Profile savedProfile = profileRepository.save(profile);
        return ProfileResponseResource.builder().userId(savedProfile.getUserId()).build();
    }

    @Override
    public String updateProfile(ProfileCreateResource profileUpdateResource) {
        Optional<Profile> profileOptional = profileRepository.findById(profileUpdateResource.getUserId());
        if (profileOptional.isEmpty()) {
            log.error("Profile not found with the user id: {}", profileUpdateResource.getUserId());
            throw new NotFoundException("Profile not found");
        }
        Profile profileEntity = profileOptional.get();
        BeanUtils.copyProperties(profileUpdateResource, profileEntity, CustomBeanUtils.getNullPropertyNames(profileUpdateResource));
        Profile updatedProfileEntity = profileRepository.save(profileEntity);
        return updatedProfileEntity.getUserId();
    }

    @Override
    public String deleteProfile(String userId) {
        try {
            Optional<Profile> profileOptional = profileRepository.findById(userId);
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
        Profile profile = profileOptional.get();
        ProfileResponseResource profileResponseResource = ProfileResponseResource.builder().build();
        BeanUtils.copyProperties(profile, profileResponseResource);
        return profileResponseResource;
    }

}
