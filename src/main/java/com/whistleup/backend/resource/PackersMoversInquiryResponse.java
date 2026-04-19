package com.whistleup.backend.resource;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PackersMoversInquiryResponse {
    private Long id;
    private String status;
    private LocalDateTime createdAt;
}
