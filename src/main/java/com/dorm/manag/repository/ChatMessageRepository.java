package com.dorm.manag.repository;

import com.dorm.manag.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    // Get recent messages (for initial load)
    List<ChatMessage> findTop100ByOrderBySentAtDesc();
    
    // Get messages after a certain time (for polling)
    List<ChatMessage> findBySentAtAfterOrderBySentAtAsc(LocalDateTime after);
    
    // Get messages before a certain time (for pagination/load more)
    List<ChatMessage> findTop50BySentAtBeforeOrderBySentAtDesc(LocalDateTime before);
    
    // Get all messages ordered by time
    List<ChatMessage> findAllByOrderBySentAtAsc();
    
    // Paginated messages
    Page<ChatMessage> findAllByOrderBySentAtDesc(Pageable pageable);
}

