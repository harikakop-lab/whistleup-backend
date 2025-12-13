package com.whistleup.backend.resource;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ComplaintImageResponse {

    private Long imageId;
    private String fileName;
    private String contentType;
    private String imageUrl;
}
