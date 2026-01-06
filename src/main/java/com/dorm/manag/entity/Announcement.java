package com.dorm.manag.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "announcements")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author; // Admin who created the announcement

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "announcement_type")
    private AnnouncementType type = AnnouncementType.GENERAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    private AnnouncementPriority priority = AnnouncementPriority.NORMAL;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_urgent", nullable = false)
    private Boolean isUrgent = false;

    @Column(name = "is_pinned", nullable = false)
    private Boolean isPinned = false;

    // Target audience
    @Column(name = "target_audience")
    private String targetAudience = "ALL";

    @Column(name = "target_rooms")
    private String targetRooms; // Comma-separated room numbers

    @Column(name = "target_floors")
    private String targetFloors; // Comma-separated floor numbers

    // Scheduling
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "scheduled_for")
    private LocalDateTime scheduledFor; // For future publishing

    // Tracking
    @Column(name = "view_count")
    private Long viewCount = 0L;

    @Column(name = "acknowledgment_required", nullable = false)
    private Boolean acknowledgmentRequired = false;

    @Column(name = "acknowledgment_count")
    private Long acknowledgmentCount = 0L;

    // Attachments and media
    @Column(name = "attachments")
    private String attachments; // JSON array of file paths

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "external_link")
    private String externalLink;

    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_modified_by_id")
    private Long lastModifiedById;

    // Notification settings
    @Column(name = "send_push_notification", nullable = false)
    private Boolean sendPushNotification = false;

    @Column(name = "send_email_notification", nullable = false)
    private Boolean sendEmailNotification = false;

    @Column(name = "notification_sent", nullable = false)
    private Boolean notificationSent = false;

    @Column(name = "notification_sent_at")
    private LocalDateTime notificationSentAt;

    // Categories and tags
    @Column(name = "category")
    private String category; // MAINTENANCE, EVENTS, RULES, NEWS, etc.

    @Column(name = "tags")
    private String tags; // Comma-separated tags

    @Column(name = "language")
    private String language = "PL"; // PL, EN

    // Constructors
    public Announcement(User author, String title, String content) {
        this.author = author;
        this.title = title;
        this.content = content;
        this.publishedAt = LocalDateTime.now();
        this.isActive = true;
    }

    public Announcement(User author, String title, String content, AnnouncementType type) {
        this(author, title, content);
        this.type = type;
        this.priority = type.getDefaultPriority();
        this.isUrgent = type.isUrgentByDefault();
    }

    // Helper methods
    public boolean isPublished() {
        LocalDateTime now = LocalDateTime.now();
        return isActive &&
                (publishedAt == null || publishedAt.isBefore(now) || publishedAt.isEqual(now)) &&
                (scheduledFor == null || scheduledFor.isBefore(now) || scheduledFor.isEqual(now));
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isScheduled() {
        return scheduledFor != null && LocalDateTime.now().isBefore(scheduledFor);
    }

    public boolean isVisible() {
        return isPublished() && !isExpired() && isActive;
    }

    public boolean isTargetedTo(User user) {
        if ("ALL".equals(targetAudience)) {
            return true;
        }

        if ("STUDENTS".equals(targetAudience)) {
            return user.getRole() == Role.STUDENT;
        }

        if ("STAFF".equals(targetAudience)) {
            return user.getRole().hasReceptionistPrivileges();
        }

        if ("SPECIFIC_ROOMS".equals(targetAudience) && targetRooms != null) {
            String userRoom = user.getRoomNumber();
            return userRoom != null && targetRooms.contains(userRoom);
        }

        if (targetFloors != null) {
            String userRoom = user.getRoomNumber();
            if (userRoom != null && userRoom.length() > 0) {
                String userFloor = userRoom.substring(0, 1);
                return targetFloors.contains(userFloor);
            }
        }

        return false;
    }

    public void incrementViewCount() {
        this.viewCount = (this.viewCount == null ? 0L : this.viewCount) + 1;
    }

    public void incrementAcknowledgmentCount() {
        this.acknowledgmentCount = (this.acknowledgmentCount == null ? 0L : this.acknowledgmentCount) + 1;
    }

    public void publish() {
        this.publishedAt = LocalDateTime.now();
        this.isActive = true;
    }

    public void schedule(LocalDateTime scheduleTime) {
        this.scheduledFor = scheduleTime;
        this.isActive = true;
    }

    public void expire() {
        this.expiresAt = LocalDateTime.now();
    }

    public void pin() {
        this.isPinned = true;
    }

    public void unpin() {
        this.isPinned = false;
    }

    public void archive() {
        this.isActive = false;
    }

    public long getDaysUntilExpiration() {
        if (expiresAt == null)
            return Long.MAX_VALUE;
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), expiresAt);
    }

    public long getHoursSincePublished() {
        if (publishedAt == null)
            return 0;
        return java.time.temporal.ChronoUnit.HOURS.between(publishedAt, LocalDateTime.now());
    }

    public double getAcknowledgmentRate() {
        if (viewCount == null || viewCount == 0)
            return 0.0;
        if (acknowledgmentCount == null)
            return 0.0;
        return (double) acknowledgmentCount / viewCount * 100.0;
    }

    public String getStatusDisplay() {
        if (!isActive)
            return "Archived";
        if (isScheduled())
            return "Scheduled";
        if (isExpired())
            return "Expired";
        if (isPublished())
            return "Published";
        return "Draft";
    }

    // Enums for announcement types and priorities
    public enum AnnouncementType {
        GENERAL("General", AnnouncementPriority.NORMAL, false),
        MAINTENANCE("Maintenance", AnnouncementPriority.HIGH, false),
        EMERGENCY("Emergency", AnnouncementPriority.CRITICAL, true),
        EVENT("Event", AnnouncementPriority.NORMAL, false),
        RULE_CHANGE("Rule Change", AnnouncementPriority.HIGH, false),
        FACILITY_UPDATE("Facility Update", AnnouncementPriority.NORMAL, false),
        PAYMENT_REMINDER("Payment Reminder", AnnouncementPriority.HIGH, false),
        NEWS("News", AnnouncementPriority.LOW, false);

        private final String displayName;
        private final AnnouncementPriority defaultPriority;
        private final boolean urgentByDefault;

        AnnouncementType(String displayName, AnnouncementPriority defaultPriority, boolean urgentByDefault) {
            this.displayName = displayName;
            this.defaultPriority = defaultPriority;
            this.urgentByDefault = urgentByDefault;
        }

        public String getDisplayName() {
            return displayName;
        }

        public AnnouncementPriority getDefaultPriority() {
            return defaultPriority;
        }

        public boolean isUrgentByDefault() {
            return urgentByDefault;
        }
    }

    public enum AnnouncementPriority {
        LOW("Low", 1, "#00AA00"),
        NORMAL("Normal", 2, "#0066CC"),
        HIGH("High", 3, "#FF9900"),
        CRITICAL("Critical", 4, "#FF0000");

        private final String displayName;
        private final int level;
        private final String color;

        AnnouncementPriority(String displayName, int level, String color) {
            this.displayName = displayName;
            this.level = level;
            this.color = color;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getLevel() {
            return level;
        }

        public String getColor() {
            return color;
        }
    }

    @Override
    public String toString() {
        return "Announcement{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", type=" + type +
                ", priority=" + priority +
                ", isActive=" + isActive +
                ", isUrgent=" + isUrgent +
                ", targetAudience='" + targetAudience + '\'' +
                ", publishedAt=" + publishedAt +
                ", viewCount=" + viewCount +
                '}';
    }
}