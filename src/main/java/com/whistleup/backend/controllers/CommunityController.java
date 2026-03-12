package com.whistleup.backend.controllers;

import com.whistleup.backend.resource.CommunityFeedResponseResource;
import com.whistleup.backend.service.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/whistleup/community")
@CrossOrigin("*")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    @GetMapping("/feed/{buildingId}")
    public ResponseEntity<CommunityFeedResponseResource> getFeed(@PathVariable String buildingId) {
        return ResponseEntity.ok(communityService.getFeedByBuilding(buildingId));
    }
}
