package com.whistleup.backend.controllers;

import com.whistleup.backend.resource.FlatRequestResource;
import com.whistleup.backend.resource.FlatResponseResource;
import com.whistleup.backend.service.FlatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/whistleup/flat")
public class FlatController {

    private final FlatService flatService;

    public FlatController(FlatService flatService) {
        this.flatService = flatService;
    }

   @GetMapping("")
   public ResponseEntity<List<FlatResponseResource>> getAllFlats() {
        List<FlatResponseResource> flatResponseResourceList = flatService.getAllFlats();
        return new ResponseEntity<>(flatResponseResourceList, HttpStatus.OK);
   }

    @GetMapping("/{buildingId}")
    public ResponseEntity<List<String>> getFlatsByBuildingId(@PathVariable String buildingId) {
        List<String> flatResponseResourceList = flatService.getAllFlatsByBuildingId(buildingId);
        return new ResponseEntity<>(flatResponseResourceList, HttpStatus.OK);
    }

    @GetMapping("/{flatId}")
    public ResponseEntity<FlatResponseResource> getFlatDetailsById(@PathVariable("flatId") Long flatId) {
        FlatResponseResource flatResponseResource = flatService.getFlatDetailsById(flatId);
        return new ResponseEntity<>(flatResponseResource, HttpStatus.OK);
    }

    @GetMapping("/flatNumber")
    public ResponseEntity<FlatResponseResource> getFlatDetailsByFlatNumber(@RequestParam("flatNumber") String flatNumber) {
        FlatResponseResource flatResponseResource = flatService.getFlatDetailsByFlatNumber(flatNumber);
        return new ResponseEntity<>(flatResponseResource, HttpStatus.OK);
    }

    @PostMapping("/add")
    public ResponseEntity<FlatResponseResource> addFlatDetails(@RequestBody FlatRequestResource flatRequestResource) {
        FlatResponseResource flatResponseResource = flatService.addFlatDetails(flatRequestResource);
        return new ResponseEntity<>(flatResponseResource, HttpStatus.OK);
    }

    @PutMapping("/update/{flatId}")
    public ResponseEntity<FlatResponseResource> updateFlatDetails(@PathVariable("flatId") Long flatId, FlatRequestResource flatRequestResource) {
        FlatResponseResource flatResponseResource = flatService.updateFlateDetails(flatId, flatRequestResource);
        return new ResponseEntity<>(flatResponseResource, HttpStatus.OK);
    }

    @DeleteMapping("/delete/{flatId}")
    public ResponseEntity<String> deleteFlatDetails(@PathVariable("flatId") Long flatId) {
        flatService.deleteFlatDetails(flatId);
        return new ResponseEntity<>("deleted", HttpStatus.OK);

    }

}
