package com.whistleup.backend.service;

import com.whistleup.backend.entity.Users;
import com.whistleup.backend.resource.UsersRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface UserService extends UserDetailsService {
    void createUser(UsersRequest usersRequest);
}