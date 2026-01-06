package com.dorm.manag.service;

import com.dorm.manag.dto.MessageDto;
import com.dorm.manag.entity.Message;
import com.dorm.manag.entity.MessageType;
import com.dorm.manag.entity.User;
import com.dorm.manag.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;

    @Transactional
    public MessageDto sendMessage(User sender, String subject, String content, MessageType type, User recipient) {
        log.info("User {} sending message: {}", sender.getUsername(), subject);

        Message message = new Message(sender, subject, content, type);

        if (recipient != null) {
            message.setRecipient(recipient);
        }

        Message savedMessage = messageRepository.save(message);
        log.info("Message sent with ID: {}", savedMessage.getId());

        return convertToDto(savedMessage);
    }

    @Transactional
    public MessageDto sendMessageToAdmin(User sender, String subject, String content, MessageType type) {
        log.info("User {} sending message to admin: {}", sender.getUsername(), subject);

        Message message = new Message(sender, subject, content, type);
        message.setRecipientDepartment("ADMIN");

        Message savedMessage = messageRepository.save(message);
        return convertToDto(savedMessage);
    }

    @Transactional
    public MessageDto replyToMessage(Long parentMessageId, User sender, String content) {
        Message parentMessage = messageRepository.findById(parentMessageId)
                .orElseThrow(() -> new RuntimeException("Parent message not found"));

        log.info("Replying to message {} by {}. Original sender: {}, Original recipient: {}, Original recipientDepartment: {}", 
                parentMessageId, sender.getUsername(), 
                parentMessage.getSender() != null ? parentMessage.getSender().getUsername() : "null",
                parentMessage.getRecipient() != null ? parentMessage.getRecipient().getUsername() : "null",
                parentMessage.getRecipientDepartment());

        Message reply = parentMessage.createReply(sender, content);
        
        log.info("Reply created. Reply sender: {}, Reply recipient: {}, Reply recipientDepartment: {}", 
                reply.getSender() != null ? reply.getSender().getUsername() : "null",
                reply.getRecipient() != null ? reply.getRecipient().getUsername() : "null",
                reply.getRecipientDepartment());
        
        Message savedReply = messageRepository.save(reply);
        messageRepository.save(parentMessage);

        log.info("Reply sent to message {} by {}. Reply ID: {}", parentMessageId, sender.getUsername(), savedReply.getId());
        return convertToDto(savedReply);
    }

    @Transactional(readOnly = true)
    public List<MessageDto> getInboxMessages(User user) {
        return messageRepository.findByRecipientOrderBySentAtDesc(user).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MessageDto> getAdminInboxMessages(User admin) {
        // Get messages where recipient is admin OR recipientDepartment is ADMIN
        List<Message> messagesToAdmin = messageRepository.findByRecipientDepartmentOrderBySentAtDesc("ADMIN");
        List<Message> messagesToUser = messageRepository.findByRecipientOrderBySentAtDesc(admin);
        
        // Combine and deduplicate
        List<Message> allMessages = new java.util.ArrayList<>(messagesToAdmin);
        for (Message msg : messagesToUser) {
            if (!allMessages.contains(msg)) {
                allMessages.add(msg);
            }
        }
        
        // Sort by sentAt descending
        allMessages.sort((a, b) -> {
            if (a.getSentAt() == null && b.getSentAt() == null) return 0;
            if (a.getSentAt() == null) return 1;
            if (b.getSentAt() == null) return -1;
            return b.getSentAt().compareTo(a.getSentAt());
        });
        
        return allMessages.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MessageDto> getSentMessages(User user) {
        List<Message> messages = messageRepository.findBySenderOrderBySentAtDesc(user);
        log.info("Found {} sent messages for user {}", messages.size(), user.getUsername());
        for (Message msg : messages) {
            log.debug("Message ID: {}, Subject: {}, Recipient: {}, RecipientDepartment: {}", 
                    msg.getId(), msg.getSubject(),
                    msg.getRecipient() != null ? msg.getRecipient().getUsername() : "null",
                    msg.getRecipientDepartment());
        }
        return messages.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MessageDto> getAllUserMessages(User user) {
        return messageRepository.findByUserOrderBySentAtDesc(user).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MessageDto> getUnreadMessages(User user) {
        return messageRepository.findUnreadByUser(user).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MessageDto> getMessageThread(String threadId) {
        return messageRepository.findByThreadIdOrderBySentAtAsc(threadId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MessageDto> getMessagesRequiringResponse() {
        return messageRepository.findMessagesRequiringResponse().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MessageDto> getOverdueMessages() {
        return messageRepository.findOverdueMessages(LocalDateTime.now()).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<MessageDto> getMessageById(Long id) {
        return messageRepository.findById(id)
                .map(this::convertToDto);
    }

    @Transactional
    public MessageDto markAsRead(Long messageId, User user) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (message.getRecipient() != null && !message.getRecipient().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        message.markAsRead();
        Message savedMessage = messageRepository.save(message);
        return convertToDto(savedMessage);
    }

    @Transactional
    public MessageDto markAsResolved(Long messageId, User user) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        message.markAsResolved();
        Message savedMessage = messageRepository.save(message);
        return convertToDto(savedMessage);
    }

    @Transactional
    public MessageDto assignMessage(Long messageId, User assignee) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        message.assignTo(assignee);
        Message savedMessage = messageRepository.save(message);
        return convertToDto(savedMessage);
    }

    @Transactional
    public void deleteMessage(Long messageId, User user) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        // Check if user is sender
        boolean isSender = message.getSender().getId().equals(user.getId());
        
        // Check if user is recipient
        boolean isRecipient = message.getRecipient() != null && 
                             message.getRecipient().getId().equals(user.getId());
        
        // Check if message is sent to admin department and user is admin
        boolean isAdminRecipient = message.getRecipientDepartment() != null && 
                                   message.getRecipientDepartment().equals("ADMIN") &&
                                   user.getRole().hasReceptionistPrivileges();
        
        // Check if user is admin and message is in their inbox
        boolean isAdminAndCanDelete = user.getRole().hasReceptionistPrivileges() && 
                                      (isRecipient || isAdminRecipient);

        if (!isSender && !isAdminAndCanDelete) {
            throw new RuntimeException("Access denied: You can only delete your own messages or messages you received");
        }

        messageRepository.delete(message);
        log.info("Message {} deleted by user {} (sender: {}, recipient: {}, admin: {})", 
                messageId, user.getUsername(), isSender, isRecipient, isAdminAndCanDelete);
    }

    @Transactional(readOnly = true)
    public MessageStatsDto getMessageStatistics() {
        long undelivered = messageRepository.countUndeliveredMessages();
        long needingResponse = messageRepository.countMessagesNeedingResponse();

        MessageStatsDto stats = new MessageStatsDto();
        stats.setUndeliveredMessages(undelivered);
        stats.setMessagesNeedingResponse(needingResponse);

        return stats;
    }

    private MessageDto convertToDto(Message message) {
        MessageDto dto = new MessageDto();
        dto.setId(message.getId());

        if (message.getSender() != null) {
            dto.setSenderId(message.getSender().getId());
            String firstName = message.getSender().getFirstName() != null ? message.getSender().getFirstName() : "";
            String lastName = message.getSender().getLastName() != null ? message.getSender().getLastName() : "";
            String fullName = (firstName + " " + lastName).trim();
            if (fullName.isEmpty()) {
                fullName = message.getSender().getUsername() != null ? message.getSender().getUsername() : "Unknown User";
            }
            dto.setSenderFullName(fullName);
            dto.setSenderEmail(message.getSender().getEmail());
        } else {
            dto.setSenderFullName("Unknown User");
        }

        if (message.getRecipient() != null) {
            dto.setRecipientId(message.getRecipient().getId());
            String firstName = message.getRecipient().getFirstName() != null ? message.getRecipient().getFirstName() : "";
            String lastName = message.getRecipient().getLastName() != null ? message.getRecipient().getLastName() : "";
            String fullName = (firstName + " " + lastName).trim();
            if (fullName.isEmpty()) {
                fullName = message.getRecipient().getUsername() != null ? message.getRecipient().getUsername() : "Unknown User";
            }
            dto.setRecipientFullName(fullName);
            dto.setRecipientEmail(message.getRecipient().getEmail());
        } else if (message.getRecipientDepartment() != null) {
            // If no recipient but has department, set department name
            dto.setRecipientFullName(message.getRecipientDepartment());
        }

        dto.setSubject(message.getSubject());
        dto.setContent(message.getContent());
        dto.setMessageType(message.getMessageType());
        dto.setStatus(message.getStatus());
        dto.setIsFromAdmin(message.getIsFromAdmin());
        dto.setRecipientDepartment(message.getRecipientDepartment());

        if (message.getParentMessage() != null) {
            dto.setParentMessageId(message.getParentMessage().getId());
        }

        dto.setThreadId(message.getThreadId());
        dto.setPriority(message.getPriority());
        dto.setIsUrgent(message.getIsUrgent());
        dto.setRequiresResponse(message.getRequiresResponse());

        dto.setSentAt(message.getSentAt());
        dto.setDeliveredAt(message.getDeliveredAt());
        dto.setReadAt(message.getReadAt());
        dto.setRepliedAt(message.getRepliedAt());
        dto.setResolvedAt(message.getResolvedAt());
        dto.setResponseDeadline(message.getResponseDeadline());

        if (message.getAssignedTo() != null) {
            dto.setAssignedToUserId(message.getAssignedTo().getId());
            dto.setAssignedToUserName(
                    message.getAssignedTo().getFirstName() + " " + message.getAssignedTo().getLastName());
        }

        dto.setAttachments(message.getAttachments());
        dto.setTags(message.getTags());
        dto.setReplyCount(message.getReplyCount());

        dto.calculateFields();
        return dto;
    }

    @lombok.Data
    public static class MessageStatsDto {
        private long undeliveredMessages;
        private long messagesNeedingResponse;
    }
}