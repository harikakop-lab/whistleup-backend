package com.whistleup.backend.controllers;

import com.whistleup.backend.entity.Users;
import com.whistleup.backend.resource.UsersRequest;
import com.whistleup.backend.service.UserService;
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
    public void register(UsersRequest usersRequest) {
      userService.createUser(usersRequest);
    }

    @PostMapping("/login")
    public String logIn(@RequestBody Users user) {
      return userService.verify(user);
    }
}
