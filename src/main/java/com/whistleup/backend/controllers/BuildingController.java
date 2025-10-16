package com.whistleup.backend.controllers;

import com.whistleup.backend.entity.BuildingDetails;
import com.whistleup.backend.resource.BuildingDetailsRequestResource;
import com.whistleup.backend.resource.BuildingDetailsResponseResource;
import com.whistleup.backend.service.BuildingDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/whistleup/building")
public class BuildingController {

    @Autowired
    BuildingDetailsService buildingDetailsService;

    @GetMapping("/{buildingId}")
    public ResponseEntity<BuildingDetailsResponseResource> getBuildingDetailsById(@PathVariable("buildingId") Long buildingId) {

        BuildingDetails buildingDetails = buildingDetailsService.getBuildingDetails(buildingId);
        return new ResponseEntity<>(buildResponseResource(buildingDetails), HttpStatus.OK);
    }

    public BuildingDetailsResponseResource buildResponseResource(BuildingDetails buildingDetails) {
        BuildingDetailsResponseResource buildingDetailsResponseResource = new BuildingDetailsResponseResource();
        buildingDetailsResponseResource.setBuildingId(buildingDetails.getBuildingId());
        buildingDetailsResponseResource.setBuildingName(buildingDetailsResponseResource.getBuildingName());
        buildingDetailsResponseResource.setBuildingAddress(buildingDetails.getBuildingAddress());
        return buildingDetailsResponseResource;
    }

    @PostMapping("/save")
    public ResponseEntity<BuildingDetailsResponseResource> saveBuildingDetails(@RequestBody BuildingDetailsRequestResource buildingDetailsRequestResource) {
        BuildingDetails buildingDetails = buildingDetailsService.saveBuildingDetails(buildingDetailsRequestResource);
        return new ResponseEntity<>(buildResponseResource(buildingDetails), HttpStatus.CREATED);
    }

    @PutMapping("/update/{buildingId}")
    public ResponseEntity<BuildingDetailsResponseResource> updateBuildingDetails(@PathVariable("buildingId") Long buildingId,
                                                                                 @RequestBody BuildingDetailsRequestResource updateRequestResource) {
        BuildingDetails buildingDetails = buildingDetailsService.updateBuildingDetails(buildingId, updateRequestResource);
        return new ResponseEntity<>(buildResponseResource(buildingDetails), HttpStatus.ACCEPTED);
    }

    @PatchMapping("/update/{buildingId}/address")
    public ResponseEntity<BuildingDetailsResponseResource> updateAddress(@PathVariable("buildingId") Long buildingId,
                                                                         @RequestBody BuildingDetailsRequestResource buildingDetailsRequestResource) {

        BuildingDetails buildingDetails = buildingDetailsService.updateBuildingAddress(buildingId, buildingDetailsRequestResource);
        return new ResponseEntity<>(buildResponseResource(buildingDetails), HttpStatus.ACCEPTED);
    }

    @DeleteMapping("/delete/{buildingId}")
    public ResponseEntity<String> deleteBuildingDetails(@PathVariable("buildingId") Long buildingId) {
        buildingDetailsService.deleteBuildingDetails(buildingId);
        return new ResponseEntity<>("Deleted", HttpStatus.OK);
    }
}
