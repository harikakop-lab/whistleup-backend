package com.whistleup.backend.controllers;

import com.whistleup.backend.resource.OwnerDetailsRequestResource;
import com.whistleup.backend.resource.OwnerDetailsResponseResource;
import com.whistleup.backend.service.OwnerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/whistleup/owner")
public class OwnerController {

    private final OwnerService ownerService;

    public OwnerController(OwnerService ownerService) {
        this.ownerService = ownerService;
    }

    @GetMapping("")
    public ResponseEntity<List<OwnerDetailsResponseResource>> getExistingOwners() {
        return new ResponseEntity<>(ownerService.getAllExistingOnwers(), HttpStatus.OK);
    }

    @GetMapping("/{ownerId}")
    public ResponseEntity<OwnerDetailsResponseResource> getOwnerDetailsById(@PathVariable("ownerId") Long ownerId) {
        return new ResponseEntity<>(ownerService.getOwnerDetailsById(ownerId), HttpStatus.OK);
    }

    @PostMapping("/save")
    public ResponseEntity<OwnerDetailsResponseResource> saveOwnerDetails(@RequestBody OwnerDetailsRequestResource ownerDetailsRequestResource) {
        return new ResponseEntity<>(ownerService.saveOwnerDetails(ownerDetailsRequestResource), HttpStatus.CREATED);
    }

    @PutMapping("/update/{ownerId}")
    public ResponseEntity<OwnerDetailsResponseResource> updateOwnerDetails(@PathVariable("ownerId") Long ownerId, @RequestBody OwnerDetailsRequestResource ownerDetailsRequestResource) {
        return new ResponseEntity<>(ownerService.updateOwnerDetails(ownerId, ownerDetailsRequestResource), HttpStatus.OK);
    }

    @DeleteMapping("/delete/{ownerId}")
    public ResponseEntity<String> deleteOwnerDetails(@PathVariable("ownerId") Long ownerId) {
        return new ResponseEntity<>(ownerService.deleteOwnerDetails(ownerId), HttpStatus.OK);
    }
}
