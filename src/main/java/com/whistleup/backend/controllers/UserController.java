package com.whistleup.backend.controllers;

import com.whistleup.backend.entity.Users;
import com.whistleup.backend.resource.UsersRequest;
import com.whistleup.backend.service.AuthenticationService;
import com.whistleup.backend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/whistleup/users")
public class UserController {

    private final UserService userService;
    private final AuthenticationService authenticationService;

    public UserController(UserService userService, AuthenticationService authenticationService) {
        this.userService = userService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody UsersRequest usersRequest) {
        if (usersRequest.getEmail() == null && usersRequest.getPhoneNumber() == null) {
            return ResponseEntity
                    .badRequest()
                    .body("Email or phone number is required");
        }
        userService.createUser(usersRequest);
        return new ResponseEntity<>("registration Successful", HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<String> logIn(@RequestBody UsersRequest user) {
        Users users = Users.builder().build();
        BeanUtils.copyProperties(user, users);
        return new ResponseEntity<>(authenticationService.authenticate(users), HttpStatus.OK);
    }
}