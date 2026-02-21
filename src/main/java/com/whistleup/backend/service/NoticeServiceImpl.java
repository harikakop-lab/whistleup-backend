package com.whistleup.backend.service;

import com.whistleup.backend.constants.IssueType;
import com.whistleup.backend.controllers.ResidentsResponse;
import com.whistleup.backend.entity.Notice;
import com.whistleup.backend.repository.NoticeRepository;
import com.whistleup.backend.repository.ProfileRepository;
import com.whistleup.backend.resource.NoticeCreateResource;
import com.whistleup.backend.resource.NoticeResponseResource;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    private final NoticeRepository noticeRepository;
    private final ProfileRepository profileRepository;
    private final NotificationSendService notificationSendService;

    @Override
    public NoticeResponseResource createNotice(NoticeCreateResource resource) {

        Notice notice = Notice.builder()
                .title(resource.getTitle())
                .description(resource.getDescription())
                .type(resource.getType())
                .profileId(resource.getProfileId())
                .createdAt(LocalDateTime.now())
                .build();
        Notice saved = noticeRepository.save(notice);
        CompletableFuture.runAsync(() -> {
            val residents = profileRepository.getListOfResidentsByBuilding(Long.valueOf(resource.getBuildingId()));
            for (ResidentsResponse resident : residents) {
                notificationSendService.notifyUser(
                        Long.valueOf(resident.getPhone()),
                        "New Notice",
                        "View the newly created notice in the notices section",
                        IssueType.INFO.name());
            }
        });
        return map(saved);
    }

    @Override
    public List<NoticeResponseResource> getNoticesByProfile(String profileId) {
        return noticeRepository.findByProfileIdOrderByCreatedAtDesc(profileId)
                .stream()
                .map(this::map)
                .toList();
    }

    private NoticeResponseResource map(Notice notice) {
        return NoticeResponseResource.builder()
                .noticeId(notice.getNoticeId())
                .title(notice.getTitle())
                .description(notice.getDescription())
                .type(notice.getType())
                .profileId(notice.getProfileId())
                .createdAt(notice.getCreatedAt())
                .build();
    }
}
