package com.whistleup.backend.controllers;

import com.whistleup.backend.resource.PollRequestResource;
import com.whistleup.backend.resource.PollResponseResource;
import com.whistleup.backend.service.PollService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("whistleup/poll")
public class PollController {

    private final PollService pollService;

    public PollController(PollService pollService) {
        this.pollService = pollService;
    }

    @GetMapping("/all/{buildingId}/")
    public ResponseEntity<List<PollResponseResource>> getAllPollsByBuildingId(@PathVariable String buildingId) {
        List<PollResponseResource> allPollsInTheBuilding = pollService.getAllPollsByBuilding(buildingId);
        return new ResponseEntity<>(allPollsInTheBuilding, HttpStatus.OK);
    }

    @PostMapping("/create")
    public ResponseEntity<PollResponseResource> createPoll(@RequestBody PollRequestResource pollRequestResource) {
        PollResponseResource pollResponseResource = pollService.createPoll(pollRequestResource);
        return new ResponseEntity<>(pollResponseResource, HttpStatus.CREATED);
    }

    @PutMapping("/update/{pollId}")
    public ResponseEntity<PollResponseResource> updatePoll(@PathVariable Long pollId, @RequestBody PollRequestResource pollRequestResource) {
        PollResponseResource pollResponseResource = pollService.updatePoll(pollId, pollRequestResource);
        return new ResponseEntity<>(pollResponseResource, HttpStatus.OK);
    }

    @PutMapping("/close/{pollId}")
    public ResponseEntity<String> closePoll(@PathVariable Long pollId, @RequestParam("isClosed") boolean isClosed) {
        pollService.closePoll(pollId, isClosed);
        return new ResponseEntity<>("Closed", HttpStatus.OK);
    }

    @DeleteMapping("/delete/{pollId}")
    public ResponseEntity<Void> deletePoll(@PathVariable Long pollId) {
        pollService.deletePoll(pollId);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
