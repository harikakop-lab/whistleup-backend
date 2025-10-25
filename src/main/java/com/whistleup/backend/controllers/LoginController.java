package com.whistleup.backend.controllers;

import com.whistleup.backend.entity.Profile;
import com.whistleup.backend.entity.Users;
import com.whistleup.backend.resource.UsersRequest;
import com.whistleup.backend.service.AuthenticationService;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/whistleup/users")
public class LoginController {

    private final AuthenticationService authenticationService;


    public LoginController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public ResponseEntity<String> logIn(@RequestBody UsersRequest user) {
        Profile profile = Profile.builder().build();
        BeanUtils.copyProperties(user, profile);
        return new ResponseEntity<>(authenticationService.authenticate(profile), HttpStatus.OK);
    }


}