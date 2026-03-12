package com.whistleup.backend.service;

import com.whistleup.backend.constants.Roles;
import com.whistleup.backend.resource.ContactResource;
import com.whistleup.backend.resource.ProfileCreateResource;
import com.whistleup.backend.resource.ProfileResponseResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

public interface ProfileService {
    ProfileResponseResource createProfile(ProfileCreateResource profileCreateResource);

    String updateProfile(ProfileCreateResource profileUpdateResource);

    String deleteProfile(String userId);

    ProfileResponseResource getProfileById(String userId);

    Roles getRole(String userName);

    ProfileResponseResource getProfileByUsername(String username);

    void uploadProfileImage(String username, MultipartFile file) throws IOException;

    Resource getProfileImage(String username) throws MalformedURLException;

    boolean doesProfileExists(String username);

    List<ContactResource> getContactsByUsername(String username);
}
