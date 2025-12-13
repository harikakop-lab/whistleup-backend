package com.whistleup.backend.controllers;

import com.whistleup.backend.resource.NoticeCreateResource;
import com.whistleup.backend.resource.NoticeResponseResource;
import com.whistleup.backend.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/whistleup/notices")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NoticeController {

    private final NoticeService noticeService;

    @PostMapping("/create")
    public ResponseEntity<NoticeResponseResource> createNotice(
            @RequestBody NoticeCreateResource resource) {
        return new ResponseEntity<>(
                noticeService.createNotice(resource),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{profileId}")
    public ResponseEntity<List<NoticeResponseResource>> getNotices(
            @PathVariable String profileId) {
        return ResponseEntity.ok(
                noticeService.getNoticesByProfile(profileId)
        );
    }
}
