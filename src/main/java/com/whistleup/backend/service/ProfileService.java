package com.whistleup.backend.service;

import com.whistleup.backend.controllers.ProfileCreateResource;
import com.whistleup.backend.controllers.ProfileResponseResource;

public interface ProfileService {
    ProfileResponseResource createProfile(ProfileCreateResource profileCreateResource);

    ProfileResponseResource updateProfile(ProfileCreateResource profileUpdateResource);
}
