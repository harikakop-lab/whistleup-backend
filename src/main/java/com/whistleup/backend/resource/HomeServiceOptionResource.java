package com.whistleup.backend.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HomeServiceOptionResource {
    private String id;
    private String title;
    private String description;
    private Integer price;
    private String image;
    private Boolean popular;
    /** Service lines (Excel C/D) with priced SKUs (E/F). */
    private List<HomeServiceCatalogLineResource> serviceLines;
}
