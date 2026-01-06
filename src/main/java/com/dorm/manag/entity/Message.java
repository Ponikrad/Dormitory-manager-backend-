package com.dorm.manag.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id")
    private User recipient; // Null for messages to admin/reception

    @Column(nullable = false)
    private String subject;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private MessageType messageType = MessageType.DIRECT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageStatus status = MessageStatus.SENT;

    @Column(name = "is_from_admin", nullable = false)
    private Boolean isFromAdmin = false;

    @Column(name = "recipient_department")
    private String recipientDepartment; // ADMIN, RECEPTION, MAINTENANCE, etc.

    // Thread management
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_message_id")
    private Message parentMessage; // For replies

    @OneToMany(mappedBy = "parentMessage", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Message> replies = new ArrayList<>();

    @Column(name = "thread_id")
    private String threadId; // To group related messages

    // Priority and urgency
    @Column(name = "priority")
    private Integer priority = 1; // 1-5, 5 being highest

    @Column(name = "is_urgent", nullable = false)
    private Boolean isUrgent = false;

    @Column(name = "requires_response", nullable = false)
    private Boolean requiresResponse = true;

    // Timestamps
    @CreationTimestamp
    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "replied_at")
    private LocalDateTime repliedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Tracking
    @Column(name = "response_deadline")
    private LocalDateTime responseDeadline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id")
    private User assignedTo; // Staff member assigned to handle the message

    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes; // Admin notes not visible to sender

    @Column(name = "attachments")
    private String attachments; // JSON array of file paths/URLs

    @Column(name = "tags")
    private String tags; // Comma-separated tags for categorization

    // Constructors
    public Message(User sender, String subject, String content, MessageType messageType) {
        this.sender = sender;
        this.subject = subject;
        this.content = content;
        this.messageType = messageType;
        this.status = MessageStatus.SENT;
        this.isFromAdmin = sender.getRole().hasReceptionistPrivileges();
        this.priority = messageType.getPriority();
        this.isUrgent = messageType.requiresUrgentResponse();
        this.recipientDepartment = messageType.getRoutingDepartment();
        this.threadId = generateThreadId();
        calculateResponseDeadline();
    }

    public Message(User sender, User recipient, String subject, String content) {
        this(sender, subject, content, MessageType.DIRECT);
        this.recipient = recipient;
        this.recipientDepartment = null;
    }

    // Helper methods
    public boolean isRead() {
        return status != MessageStatus.SENT && status != MessageStatus.DELIVERED;
    }

    public boolean isFromUser() {
        return !isFromAdmin;
    }

    public boolean needsResponse() {
        return requiresResponse && !isResolved() &&
                (status == MessageStatus.DELIVERED || status == MessageStatus.READ);
    }

    public boolean isOverdue() {
        return responseDeadline != null &&
                LocalDateTime.now().isAfter(responseDeadline) &&
                needsResponse();
    }

    public boolean isResolved() {
        return status == MessageStatus.RESOLVED;
    }

    public boolean isReply() {
        return parentMessage != null;
    }

    public boolean hasReplies() {
        return replies != null && !replies.isEmpty();
    }

    public void markAsDelivered() {
        if (status == MessageStatus.SENT) {
            this.status = MessageStatus.DELIVERED;
            this.deliveredAt = LocalDateTime.now();
        }
    }

    public void markAsRead() {
        if (status == MessageStatus.SENT || status == MessageStatus.DELIVERED) {
            this.status = MessageStatus.READ;
            this.readAt = LocalDateTime.now();
        }
    }

    public void markAsReplied() {
        this.status = MessageStatus.REPLIED;
        this.repliedAt = LocalDateTime.now();
    }

    public void markAsResolved() {
        this.status = MessageStatus.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
        this.requiresResponse = false;
    }

    public void assignTo(User staff) {
        this.assignedTo = staff;
        if (status == MessageStatus.SENT) {
            markAsDelivered();
        }
    }

    public Message createReply(User replier, String replyContent) {
        Message reply = new Message();
        reply.setSender(replier);
        
        // Determine recipient:
        // - If original message was sent to a department (recipientDepartment != null), 
        //   reply should go to the original sender
        // - If original message has a specific recipient, reply should go to that recipient
        // - If admin is replying to a user's message, reply goes to the user (original sender)
        // - If user is replying to admin's message, reply goes to admin (original sender)
        if (this.recipientDepartment != null) {
            // Message was sent to a department, reply goes to original sender
            reply.setRecipient(this.sender);
            reply.setRecipientDepartment(null);
        } else if (this.recipient != null) {
            // Message has specific recipient, reply goes to original sender
            reply.setRecipient(this.sender);
            reply.setRecipientDepartment(null);
        } else {
            // Fallback: reply to original sender
            reply.setRecipient(this.sender);
        }
        
        reply.setSubject("Re: " + this.subject);
        reply.setContent(replyContent);
        reply.setMessageType(MessageType.REPLY);
        reply.setParentMessage(this);
        reply.setThreadId(this.threadId);
        reply.setIsFromAdmin(replier.getRole().hasReceptionistPrivileges());
        reply.setPriority(this.priority);
        reply.calculateResponseDeadline();

        // Mark original message as replied
        this.markAsReplied();

        return reply;
    }

    public long getHoursUntilDeadline() {
        if (responseDeadline == null)
            return 0;
        return java.time.Duration.between(LocalDateTime.now(), responseDeadline).toHours();
    }

    public long getHoursSinceSent() {
        return java.time.Duration.between(sentAt, LocalDateTime.now()).toHours();
    }

    public int getReplyCount() {
        return replies != null ? replies.size() : 0;
    }

    private void calculateResponseDeadline() {
        if (requiresResponse && messageType != null) {
            int hours = messageType.getExpectedResponseTimeHours();
            this.responseDeadline = sentAt != null ? sentAt.plusHours(hours) : LocalDateTime.now().plusHours(hours);
        }
    }

    private String generateThreadId() {
        return "THR-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public String getDisplaySender() {
        return sender != null ? sender.getFirstName() + " " + sender.getLastName() : "Unknown";
    }

    public String getDisplayRecipient() {
        if (recipient != null) {
            return recipient.getFirstName() + " " + recipient.getLastName();
        } else if (recipientDepartment != null) {
            return recipientDepartment.charAt(0) + recipientDepartment.substring(1).toLowerCase();
        } else {
            return "Administration";
        }
    }

    // Override toString to avoid circular references
    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", senderId=" + (sender != null ? sender.getId() : null) +
                ", recipientId=" + (recipient != null ? recipient.getId() : null) +
                ", subject='" + subject + '\'' +
                ", messageType=" + messageType +
                ", status=" + status +
                ", isFromAdmin=" + isFromAdmin +
                ", priority=" + priority +
                ", isUrgent=" + isUrgent +
                ", sentAt=" + sentAt +
                ", threadId='" + threadId + '\'' +
                '}';
    }
}