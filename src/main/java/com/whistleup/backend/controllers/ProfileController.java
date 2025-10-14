package com.whistleup.backend.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/whistleup/profile")
public class ProfileController {

    @GetMapping("")
    public ResponseEntity<String> sayHello() {
        return new ResponseEntity<>("Hello, Profile", HttpStatus.OK);
    }
}
