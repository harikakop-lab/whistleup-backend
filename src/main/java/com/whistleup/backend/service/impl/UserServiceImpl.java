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

    private UserRepository userRepository;


     private AuthenticationManager authenticationManager;

     private JwtService jwtService;

    private final PasswordEncoder passwordEncoder;


    public UserServiceImpl(UserRepository userRepository, AuthenticationManager authenticationManager, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    public void createUser(UsersRequest usersRequest) {
        Users users = Users.builder().build();
        BeanUtils.copyProperties(usersRequest, users);
        users.setPassword(passwordEncoder.encode(users.getPassword()));
        userRepository.save(users);
    }

    @Override
    public String verify(Users user) {
        String email = user.getEmail();
        if(email != null) {
            Authentication authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(email, user.getPassword()));
            if (authentication.isAuthenticated()) {
                return jwtService.generateToken(user.getEmail());
            }
        }
        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(user.getPhoneNumber(), user.getPassword()));
        if (authentication.isAuthenticated()) {
            return jwtService.generateToken(user.getEmail());
        }
        return "login failed";

    }

    @Override
    public UserDetails loadUserByUsername(String emailOrPhoneNumber) {
        Users users = userRepository.findByEmailOrPhoneNumber(emailOrPhoneNumber)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with phone number or email: " + emailOrPhoneNumber
                ));
        String email = users.getEmail();

        if (email != null) {
            return org.springframework.security.core.userdetails.User
                    .withUsername(email)  // we choose username field as principal
                    .password(users.getPassword())
                    .accountLocked(false)
                    .build();
        }
        return org.springframework.security.core.userdetails.User
                .withUsername(users.getPhoneNumber())  // we choose username field as principal
                .password(users.getPassword())
                .accountLocked(false)
                .build();
    }

}


