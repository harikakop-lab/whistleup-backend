package com.whistleup.backend.resource;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PackersMoversItemSectionResource {
    private String key;
    private String title;
    private List<PackersMoversItemRowResource> rows;
}
