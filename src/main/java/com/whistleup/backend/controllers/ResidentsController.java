package com.whistleup.backend.controllers;

import com.whistleup.backend.service.ResidentsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/whistleup/residents")
@CrossOrigin("*")
public class ResidentsController {

    private final ResidentsService residentsService;

    public ResidentsController(ResidentsService residentsService) {
        this.residentsService = residentsService;
    }

    @GetMapping("/building/{buildingId}/floor/{floorNo}")
    public ResponseEntity<List<ResidentsResponse>> getAllResidentsByBuildingAndFloor(@PathVariable("buildingId") String buildingId, @PathVariable("floorNo") String floorNo) {
        List<ResidentsResponse> listOfResidents = residentsService.getAllResidentsByBuildingAndFloor(buildingId, floorNo);
        return new ResponseEntity<>(listOfResidents, HttpStatus.OK);
    }

    @GetMapping("/building/{buildingId}")
    public ResponseEntity<List<ResidentsResponse>> getAllResidentsByBuildingId(@PathVariable("buildingId") String buildingId) {
        List<ResidentsResponse> listOfResidents = residentsService.getAllResidentsByBuilding(buildingId);
        return new ResponseEntity<>(listOfResidents, HttpStatus.OK);
    }
}
