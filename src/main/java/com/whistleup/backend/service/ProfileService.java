package com.whistleup.backend.service;

import com.whistleup.backend.resource.ProfileCreateResource;
import com.whistleup.backend.resource.ProfileResponseResource;

public interface ProfileService {
    ProfileResponseResource createProfile(ProfileCreateResource profileCreateResource);

    String updateProfile(ProfileCreateResource profileUpdateResource);

    String deleteProfile(String userId);

    ProfileResponseResource getProfileById(String userId);
}
