package com.dorm.manag.service;

import com.dorm.manag.dto.ChatMessageDto;
import com.dorm.manag.entity.ChatMessage;
import com.dorm.manag.entity.User;
import com.dorm.manag.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;

    @Transactional(readOnly = true)
    public List<ChatMessageDto> getRecentMessages(User currentUser) {
        List<ChatMessage> messages = chatMessageRepository.findTop100ByOrderBySentAtDesc();
        java.util.Collections.reverse(messages);
        return messages.stream()
                .map(msg -> convertToDto(msg, currentUser))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> getMessagesAfter(LocalDateTime after, User currentUser) {
        List<ChatMessage> messages = chatMessageRepository.findBySentAtAfterOrderBySentAtAsc(after);
        return messages.stream()
                .map(msg -> convertToDto(msg, currentUser))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> getAllMessages(User currentUser) {
        List<ChatMessage> messages = chatMessageRepository.findAllByOrderBySentAtAsc();
        return messages.stream()
                .map(msg -> convertToDto(msg, currentUser))
                .collect(Collectors.toList());
    }

    @Transactional
    public ChatMessageDto sendMessage(User sender, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("Message content cannot be empty");
        }

        ChatMessage message = new ChatMessage(sender, content.trim());
        ChatMessage savedMessage = chatMessageRepository.save(message);

        log.info("User {} sent chat message: {}", sender.getUsername(),
                content.length() > 50 ? content.substring(0, 50) + "..." : content);

        return convertToDto(savedMessage, sender);
    }

    private ChatMessageDto convertToDto(ChatMessage message, User currentUser) {
        ChatMessageDto dto = new ChatMessageDto();
        dto.setId(message.getId());
        dto.setSenderId(message.getSender().getId());
        dto.setSenderName(message.getSender().getFirstName() + " " + message.getSender().getLastName());
        dto.setSenderRoomNumber(message.getSender().getRoomNumber());
        dto.setContent(message.getContent());
        dto.setSentAt(message.getSentAt());
        dto.setIsFromCurrentUser(currentUser != null &&
                message.getSender().getId().equals(currentUser.getId()));
        return dto;
    }
}
