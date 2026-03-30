package com.whistleup.backend.service;

import com.whistleup.backend.entity.Event;
import com.whistleup.backend.entity.Notice;
import com.whistleup.backend.repository.EventRepository;
import com.whistleup.backend.repository.NoticeRepository;
import com.whistleup.backend.resource.CommunityFeedItemResource;
import com.whistleup.backend.resource.CommunityFeedResponseResource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CommunityServiceImpl implements CommunityService {

    private final EventRepository eventRepository;
    private final NoticeRepository noticeRepository;

    @Override
    public CommunityFeedResponseResource getFeedByBuilding(String buildingId) {
        List<CommunityFeedItemResource> noticeItems = noticeRepository.findByBuildingIdOrderByCreatedAtDesc(buildingId)
                .stream()
                .map(this::mapNotice)
                .toList();
        List<CommunityFeedItemResource> eventItems = eventRepository.findByBuildingIdOrderByCreatedAtDesc(buildingId)
                .stream()
                .map(this::mapEvent)
                .toList();

        List<CommunityFeedItemResource> merged = java.util.stream.Stream.concat(
                        noticeItems.stream(),
                        eventItems.stream()
                )
                .sorted(Comparator.comparing(CommunityFeedItemResource::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        return CommunityFeedResponseResource.builder()
                .buildingId(buildingId)
                .items(merged)
                .build();
    }

    private CommunityFeedItemResource mapNotice(Notice notice) {
        return CommunityFeedItemResource.builder()
                .id(notice.getNoticeId() != null ? notice.getNoticeId().toString() : null)
                .type(resolveNoticeFeedType(notice.getType()))
                .title(notice.getTitle())
                .body(notice.getDescription())
                .createdAt(notice.getCreatedAt())
                .source("NOTICE")
                .build();
    }

    private CommunityFeedItemResource mapEvent(Event event) {
        return CommunityFeedItemResource.builder()
                .id(event.getEventId())
                .type("Events")
                .title(event.getTitle())
                .body(event.getDescription())
                .createdAt(event.getCreatedAt())
                .source("EVENT")
                .build();
    }

    private String resolveNoticeFeedType(String type) {
        if (Objects.isNull(type)) {
            return "Notice";
        }
        String value = type.trim().toUpperCase();
        return switch (value) {
            case "HIGH", "ALERT", "NOTICE" -> "Notice";
            case "LOW", "EVENT" -> "Events";
            default -> "Announcements";
        };
    }
}
