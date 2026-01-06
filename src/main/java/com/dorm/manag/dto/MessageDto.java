package com.dorm.manag.dto;

import com.dorm.manag.entity.MessageStatus;
import com.dorm.manag.entity.MessageType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {

    private Long id;

    private Long senderId;

    private String senderFullName;

    private String senderEmail;

    private Long recipientId;

    private String recipientFullName;

    private String recipientEmail;

    private String subject;

    private String content;

    private MessageType messageType;

    private MessageStatus status;

    private Boolean isFromAdmin;

    private String recipientDepartment;

    private Long parentMessageId;

    private String threadId;

    private Integer priority;

    private Boolean isUrgent;

    private Boolean requiresResponse;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sentAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deliveredAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime repliedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime resolvedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime responseDeadline;

    private Long assignedToUserId;

    private String assignedToUserName;

    private String attachments;

    private String tags;

    private int replyCount;

    private boolean isRead;

    private boolean isOverdue;

    private long hoursUntilDeadline;

    private long hoursSinceSent;

    public void calculateFields() {
        this.isRead = status != null && status != MessageStatus.SENT && status != MessageStatus.DELIVERED;
        
        this.isOverdue = responseDeadline != null &&
                LocalDateTime.now().isAfter(responseDeadline) &&
                requiresResponse != null && requiresResponse &&
                status != MessageStatus.RESOLVED;

        if (responseDeadline != null) {
            this.hoursUntilDeadline = java.time.Duration.between(LocalDateTime.now(), responseDeadline).toHours();
        } else {
            this.hoursUntilDeadline = 0;
        }

        if (sentAt != null) {
            this.hoursSinceSent = java.time.Duration.between(sentAt, LocalDateTime.now()).toHours();
        } else {
            this.hoursSinceSent = 0;
        }
    }
}

