package com.whistleup.backend.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Serialized into {@code Maintenance.ledgerAttachmentsJson}. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LedgerAttachmentStored {
    private String fileName;
    private String contentType;
}
