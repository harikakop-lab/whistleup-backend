package com.whistleup.backend.service.impl;

import com.whistleup.backend.entity.Users;
import com.whistleup.backend.repository.UserRepository;
import com.whistleup.backend.resource.UsersRequest;
import com.whistleup.backend.service.JwtService;
import com.whistleup.backend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void createUser(UsersRequest usersRequest) {
        Users users = Users.builder().build();
        BeanUtils.copyProperties(usersRequest, users);
        users.setPassword(passwordEncoder.encode(users.getPassword()));
        userRepository.save(users);
    }

    @Override
    public UserDetails loadUserByUsername(String emailOrPhoneNumber) {
        Users users = userRepository.findByEmailOrPhoneNumber(emailOrPhoneNumber)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with phone number or email: " + emailOrPhoneNumber
                ));

        String username = users.getEmail() != null ? users.getEmail() : users.getPhoneNumber();

        return org.springframework.security.core.userdetails.User
                .withUsername(username)
                .password(users.getPassword())
                .accountLocked(false)
                .build();
    }
}