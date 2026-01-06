package com.dorm.manag.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementDto {

    private Long id;

    private Long authorId;

    private String authorName;

    private String title;

    private String content;

    private String type; // GENERAL, MAINTENANCE, EMERGENCY, etc.

    private String priority; // LOW, NORMAL, HIGH, CRITICAL

    private Boolean isActive;

    private Boolean isUrgent;

    private Boolean isPinned;

    private String targetAudience;

    private String targetRooms;

    private String targetFloors;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime publishedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime scheduledFor;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    private Long viewCount;

    private Long acknowledgmentCount;

    private Boolean acknowledgmentRequired;

    private String imageUrl;

    private String externalLink;

    private String attachments;

    private String category;

    private String tags;

    private String language;

    private String statusDisplay;
}

