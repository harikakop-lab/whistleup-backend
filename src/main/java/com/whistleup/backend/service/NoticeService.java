package com.whistleup.backend.service;

import com.whistleup.backend.resource.NoticeCreateResource;
import com.whistleup.backend.resource.NoticeResponseResource;

import java.util.List;

public interface NoticeService {

    NoticeResponseResource createNotice(NoticeCreateResource resource);

    List<NoticeResponseResource> getNoticesByBuilding(String profileId);
}
