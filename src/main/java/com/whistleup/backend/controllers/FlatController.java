package com.whistleup.backend.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/whistleup/flat")
public class FlatController {

    @GetMapping("{flatId}")
    public ResponseEntity<String> getFlatDetails(@PathVariable Long flatId) {
        return new ResponseEntity<>("flatDetails", HttpStatus.OK);
    }

}
