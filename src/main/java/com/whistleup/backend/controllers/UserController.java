package com.whistleup.backend.controllers;

import com.whistleup.backend.entity.Users;
import com.whistleup.backend.resource.UsersRequest;
import com.whistleup.backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/whistleup/users")
public class UserController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;

    public UserController(AuthenticationManager authenticationManager, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(UsersRequest usersRequest) {
        userService.createUser(usersRequest);
        return new ResponseEntity<>("registration Successful", HttpStatus.CREATED);

    }

    @PostMapping("/login")
    public ResponseEntity<String> logIn(@RequestBody Users user) {
      return new ResponseEntity<>(userService.verify(user), HttpStatus.ACCEPTED);
    }
}
