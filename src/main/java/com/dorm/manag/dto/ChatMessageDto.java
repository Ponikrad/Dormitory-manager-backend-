package com.dorm.manag.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatMessageDto {
    private Long id;
    private Long senderId;
    private String senderName;
    private String senderRoomNumber;
    private String content;
    private LocalDateTime sentAt;
    private Boolean isFromCurrentUser;
}

