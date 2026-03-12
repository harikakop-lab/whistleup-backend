package com.whistleup.backend.controllers;

import com.whistleup.backend.entity.BuildingDetails;
import com.whistleup.backend.resource.BuildingDetailsRequestResource;
import com.whistleup.backend.resource.BuildingDetailsResponseResource;
import com.whistleup.backend.resource.BuildingServices;
import com.whistleup.backend.service.BuildingDetailsService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/whistleup/building")
@CrossOrigin("*")
public class BuildingController {

    @Autowired
    BuildingDetailsService buildingDetailsService;

    @GetMapping("/")
    public ResponseEntity<List<BuildingDetailsResponseResource>> getExistingBuildingDetails() {
        return new ResponseEntity<>(buildingDetailsService.getExistingBuildingsInformation(), HttpStatus.OK);
    }

    @GetMapping("/all")
    public ResponseEntity<List<BuildingDetailsResponseResource>> getBuildingDetailsDropDown() {
        return new ResponseEntity<>(buildingDetailsService.getBuildingDetailsDropDown(), HttpStatus.OK);
    }

    @GetMapping("/{buildingId}")
    public ResponseEntity<BuildingDetailsResponseResource> getBuildingDetailsById(@PathVariable("buildingId") Long buildingId) {
        log.info("Received call from FE: {}", buildingId);
        BuildingDetails buildingDetails = buildingDetailsService.getBuildingDetails(buildingId);
        return new ResponseEntity<>(buildResponseResource(buildingDetails), HttpStatus.OK);
    }

    public BuildingDetailsResponseResource buildResponseResource(BuildingDetails buildingDetails) {
        BuildingDetailsResponseResource buildingDetailsResponseResource = new BuildingDetailsResponseResource();
        BeanUtils.copyProperties(buildingDetails, buildingDetailsResponseResource);
        buildingDetailsResponseResource.setBuildingId(buildingDetails.getBuildingId());
        buildingDetailsResponseResource.setBuildingName(buildingDetails.getBuildingName());
        buildingDetailsResponseResource.setBuildingAddress(buildingDetails.getBuildingAddress());
        buildingDetailsResponseResource.setFloors(buildingDetails.getFloors());
        return buildingDetailsResponseResource;
    }

    @PostMapping("/create")
    public ResponseEntity<BuildingDetailsResponseResource> saveBuildingDetails(@Valid @RequestBody BuildingDetailsRequestResource buildingDetailsRequestResource) {
        BuildingDetails buildingDetails = buildingDetailsService.saveBuildingDetails(buildingDetailsRequestResource);
        return new ResponseEntity<>(buildResponseResource(buildingDetails), HttpStatus.CREATED);
    }

    @PutMapping("/update/{buildingId}")
    public ResponseEntity<BuildingDetailsResponseResource> updateBuildingDetails(@PathVariable("buildingId") Long buildingId,
                                                                                 @RequestBody BuildingDetailsRequestResource updateRequestResource) {
        BuildingDetails buildingDetails = buildingDetailsService.updateBuildingDetails(buildingId, updateRequestResource);
        return new ResponseEntity<>(buildResponseResource(buildingDetails), HttpStatus.OK);
    }

    @PutMapping("/update/services")
    public ResponseEntity<Void> updateWatchmanDetails(@RequestBody BuildingServices buildingServices) {
        buildingDetailsService.updateBuildingServices(Long.valueOf(buildingServices.getBuildingId()), buildingServices);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PatchMapping("/update/{buildingId}/address")
    public ResponseEntity<BuildingDetailsResponseResource> updateAddress(@PathVariable("buildingId") Long buildingId,
                                                                         @RequestBody BuildingDetailsRequestResource buildingDetailsRequestResource) {

        BuildingDetails buildingDetails = buildingDetailsService.updateBuildingAddress(buildingId, buildingDetailsRequestResource);
        return new ResponseEntity<>(buildResponseResource(buildingDetails), HttpStatus.ACCEPTED);
    }

    @GetMapping("/profile/{username}")
    public ResponseEntity<BuildingDetailsResponseResource> getBuildingDetailsByProfileId(@PathVariable("username") String username) {
        BuildingDetails buildingDetails = buildingDetailsService.getBuildingServicesByProfileId(username);
        return new ResponseEntity<>(buildResponseResource(buildingDetails), HttpStatus.OK);
    }

    @DeleteMapping("/delete/{buildingId}")
    public ResponseEntity<String> deleteBuildingDetails(@PathVariable("buildingId") Long buildingId) {
        buildingDetailsService.deleteBuildingDetails(buildingId);
        return new ResponseEntity<>("Deleted", HttpStatus.OK);
    }
}
