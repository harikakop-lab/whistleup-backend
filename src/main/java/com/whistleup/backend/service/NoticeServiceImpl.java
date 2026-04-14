package com.whistleup.backend.service;

import com.whistleup.backend.constants.IssueType;
import com.whistleup.backend.constants.NoticeAudience;
import com.whistleup.backend.constants.Roles;
import com.whistleup.backend.entity.Notice;
import com.whistleup.backend.entity.Profile;
import com.whistleup.backend.repository.NoticeRepository;
import com.whistleup.backend.repository.ProfileRepository;
import com.whistleup.backend.resource.NoticeCreateResource;
import com.whistleup.backend.resource.NoticeResponseResource;
import lombok.RequiredArgsConstructor;
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
        NoticeAudience audience = resource.getAudience() != null
                ? resource.getAudience()
                : NoticeAudience.ALL_RESIDENTS;
        Notice notice = Notice.builder()
                .title(resource.getTitle())
                .description(resource.getDescription())
                .type(resource.getType())
                .profileId(resource.getProfileId())
                .buildingId(resource.getBuildingId())
                .audience(audience)
                .createdAt(LocalDateTime.now())
                .build();
        Notice noticeEntity = noticeRepository.save(notice);
        CompletableFuture.runAsync(() -> {
            List<Profile> recipients = resolveRecipients(resource.getBuildingId(), audience);
            for (Profile profile : recipients) {
                try {
                    notificationSendService.notifyUser(
                            Long.valueOf(profile.getPhone()),
                            noticeEntity.getTitle(),
                            "View the newly created notice in the notices section",
                            IssueType.INFO.name());
                } catch (Exception ignore) {
                    // Some dev/test profiles may have non-numeric phones; skip push for those entries.
                }
            }
        });
        return map(noticeEntity);
    }

    @Override
    public List<NoticeResponseResource> getNoticesByBuilding(String buildingId) {
        return noticeRepository.findByBuildingIdOrderByCreatedAtDesc(buildingId)
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
                .audience(notice.getAudience())
                .createdAt(notice.getCreatedAt())
                .build();
    }

    private List<Profile> resolveRecipients(String buildingId, NoticeAudience audience) {
        if (audience == NoticeAudience.ALL_OWNERS) {
            return profileRepository.findByBuildingId(buildingId)
                    .stream()
                    .filter(p -> p.getRole() == Roles.ADMIN || p.getRole() == Roles.OWNER)
                    .toList();
        }
        if (audience == NoticeAudience.ALL_TENANTS) {
            return profileRepository.findByBuildingIdAndRole(buildingId, Roles.USER);
        }
        return profileRepository.findByBuildingId(buildingId);
    }
}
