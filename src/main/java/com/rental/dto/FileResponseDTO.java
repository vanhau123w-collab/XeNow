package com.rental.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class FileResponseDTO {
    private String fileName;
    private String folder;
    private String fileUrl;
    private Long size;
    private Instant uploadedAt;
}
