package com.whistleup.backend.service.impl;

import com.whistleup.backend.entity.Profile;
import com.whistleup.backend.repository.ProfileRepository;
import com.whistleup.backend.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class LoginServiceImpl implements UserDetailsService {

    private final ProfileRepository profileRepository;

    public LoginServiceImpl(ProfileRepository profileRepository, UserRepository userRepository) {

        this.profileRepository = profileRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String emailOrPhoneNumber) {
        Profile profile = profileRepository.findByEmailOrPhone(emailOrPhoneNumber)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with phone number or email: " + emailOrPhoneNumber
                ));

        String username = profile.getEmail() != null ? profile.getEmail() : profile.getPhone();

        return org.springframework.security.core.userdetails.User
                .withUsername(username)
                .password(profile.getPin())
                .accountLocked(false)
                .build();
    }
}