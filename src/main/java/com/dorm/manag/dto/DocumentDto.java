package com.dorm.manag.dto;

import com.dorm.manag.entity.DocumentType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDto {

    private Long id;
    private String title;
    private String description;
    private DocumentType documentType;
    private String fileName;
    private Long fileSize;
    private String formattedFileSize;
    private String mimeType;
    private String accessLevel;
    private String version;
    private String versionNotes;
    private Boolean isActive;
    private Boolean isFeatured;
    private Boolean requiresAcknowledgment;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validFrom;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validUntil;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approvedAt;

    private Long viewCount;
    private Long downloadCount;
    private String language;
    private String category;
    private String tags;
    private String uploadedByName;
    private String approvedByName;

    private boolean expired;
    private boolean needsReview;
    private boolean approved;
    private boolean fileDocument;
    private boolean textDocument;
    private long daysUntilExpiry;
    private boolean popular;
}