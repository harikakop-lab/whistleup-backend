package com.whistleup.backend.controllers;

import com.whistleup.backend.resource.EventCreateResource;
import com.whistleup.backend.resource.EventResponseResource;
import com.whistleup.backend.service.EventService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/whistleup/events")
@CrossOrigin(origins = "*")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping("/create")
    public ResponseEntity<EventResponseResource> createEvent(
            @RequestBody EventCreateResource request) {
        return new ResponseEntity<>(eventService.createEvent(request), HttpStatus.CREATED);
    }

    @PatchMapping("/update/{eventId}")
    public ResponseEntity<EventResponseResource> updateEvent(
            @PathVariable String eventId,
            @RequestBody EventCreateResource request) {
        return ResponseEntity.ok(eventService.updateEvent(eventId, request));
    }

    @GetMapping("/{profileId}")
    public ResponseEntity<List<EventResponseResource>> getEventsByProfileId(
            @PathVariable String profileId) {
        return ResponseEntity.ok(eventService.getEventsByProfileId(profileId));
    }
}
