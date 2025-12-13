package com.whistleup.backend.service;

import com.whistleup.backend.entity.Notice;
import com.whistleup.backend.repository.NoticeRepository;
import com.whistleup.backend.resource.NoticeCreateResource;
import com.whistleup.backend.resource.NoticeResponseResource;
import com.whistleup.backend.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    private final NoticeRepository noticeRepository;

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
