package com.whistleup.backend.service.impl;

import com.whistleup.backend.controllers.ProfileCreateResource;
import com.whistleup.backend.controllers.ProfileResponseResource;
import com.whistleup.backend.entity.Profile;
import com.whistleup.backend.service.ProfileService;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;

    public ProfileServiceImpl(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Override
    public ProfileResponseResource createProfile(ProfileCreateResource profileCreateResource) {
        if (profileRepository.findByEmail(profileCreateResource.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        Profile profile = Profile.builder().build();
        BeanUtils.copyProperties(profileCreateResource, profile);
        Profile savedProfile = profileRepository.save(profile);
        return ProfileResponseResource.builder().userId(savedProfile.getUserId()).build();
    }

    @Override
    public ProfileResponseResource updateProfile(ProfileCreateResource profileUpdateResource) {
        return ProfileResponseResource.builder().build();
    }
}
