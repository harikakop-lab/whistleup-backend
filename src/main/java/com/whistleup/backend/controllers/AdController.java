package com.whistleup.backend.controllers;

import com.whistleup.backend.service.AdsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/whistleup/ad")
@CrossOrigin("*")
@RequiredArgsConstructor
public class AdController {

    private final AdsService adsService;

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> registerComplaint(@RequestPart("city") String city,
                                                        @RequestPart(value = "files", required = false) MultipartFile[] files) throws Exception {
        String response = adsService.uploadAdWithCity(city, files);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
