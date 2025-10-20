package com.whistleup.backend.controllers;

import com.whistleup.backend.entity.Users;
import com.whistleup.backend.resource.UsersRequest;
import com.whistleup.backend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/whistleup/users")
public class UserController {

    private final UserService userService;

    public UserController(AuthenticationManager authenticationManager, UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(UsersRequest usersRequest) {
        if(usersRequest.getEmail() == null && usersRequest.getPhoneNumber() == null) {
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
      return new ResponseEntity<>(userService.verify(users), HttpStatus.ACCEPTED);
    }
}
