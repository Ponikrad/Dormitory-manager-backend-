package com.dorm.manag.service;

import com.dorm.manag.dto.AnnouncementDto;
import com.dorm.manag.entity.Announcement;
import com.dorm.manag.entity.User;
import com.dorm.manag.repository.AnnouncementRepository;
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
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;

    @Transactional
    public Announcement createAnnouncement(User author, String title, String content,
            Announcement.AnnouncementType type, Announcement.AnnouncementPriority priority,
            String targetAudience, boolean isPinned, boolean isUrgent) {
        log.info("Creating announcement: {} by {}", title, author.getUsername());

        Announcement announcement = new Announcement(author, title, content, type);
        announcement.setPriority(priority);
        announcement.setTargetAudience(targetAudience != null ? targetAudience : "ALL");
        announcement.setIsPinned(isPinned);
        announcement.setIsUrgent(isUrgent);
        announcement.publish();

        Announcement saved = announcementRepository.save(announcement);
        log.info("Announcement created with ID: {}", saved.getId());

        return saved;
    }

    @Transactional
    public Announcement scheduleAnnouncement(User author, String title, String content,
            Announcement.AnnouncementType type, LocalDateTime scheduledFor) {
        log.info("Scheduling announcement: {} for {}", title, scheduledFor);

        Announcement announcement = new Announcement(author, title, content, type);
        announcement.schedule(scheduledFor);

        return announcementRepository.save(announcement);
    }

    @Transactional(readOnly = true)
    public List<AnnouncementDto> getPublishedAnnouncements(User user) {
        List<Announcement> allPublished = announcementRepository.findPublishedAnnouncements(LocalDateTime.now());

        return allPublished.stream()
                .filter(a -> a.isTargetedTo(user))
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AnnouncementDto> getPinnedAnnouncements() {
        return announcementRepository.findPinnedAnnouncements().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AnnouncementDto> getAnnouncementsByType(Announcement.AnnouncementType type) {
        return announcementRepository.findByTypeOrderByPublishedAtDesc(type).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AnnouncementDto> getUrgentAnnouncements() {
        return announcementRepository.findByIsUrgentAndIsActiveOrderByPublishedAtDesc(true, true).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AnnouncementDto> getScheduledAnnouncements() {
        return announcementRepository.findScheduledAnnouncements(LocalDateTime.now()).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AnnouncementDto> searchAnnouncements(String searchTerm) {
        return announcementRepository.searchAnnouncements(searchTerm).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<AnnouncementDto> getAnnouncementById(Long id) {
        return announcementRepository.findByIdWithAuthor(id)
                .map(this::convertToDto);
    }

    @Transactional
    public Announcement updateAnnouncement(Long id, String title, String content,
            Announcement.AnnouncementType type, Announcement.AnnouncementPriority priority,
            String targetAudience, boolean isPinned, boolean isUrgent, User modifier) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Announcement not found"));

        announcement.setTitle(title);
        announcement.setContent(content);
        announcement.setType(type);
        announcement.setPriority(priority);
        announcement.setTargetAudience(targetAudience);
        announcement.setIsPinned(isPinned);
        announcement.setIsUrgent(isUrgent);
        announcement.setLastModifiedById(modifier.getId());

        log.info("Announcement {} updated by {}", id, modifier.getUsername());
        return announcementRepository.save(announcement);
    }

    @Transactional
    public void incrementViewCount(Long id) {
        announcementRepository.findById(id).ifPresent(announcement -> {
            announcement.incrementViewCount();
            announcementRepository.save(announcement);
        });
    }

    @Transactional
    public void acknowledgeAnnouncement(Long id, User user) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Announcement not found"));

        announcement.incrementAcknowledgmentCount();
        announcementRepository.save(announcement);

        log.info("User {} acknowledged announcement {}", user.getUsername(), id);
    }

    @Transactional
    public void pinAnnouncement(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Announcement not found"));

        announcement.pin();
        announcementRepository.save(announcement);
    }

    @Transactional
    public void unpinAnnouncement(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Announcement not found"));

        announcement.unpin();
        announcementRepository.save(announcement);
    }

    @Transactional
    public void archiveAnnouncement(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Announcement not found"));

        announcement.archive();
        announcementRepository.save(announcement);

        log.info("Announcement {} archived", id);
    }

    @Transactional
    public void deleteAnnouncement(Long id) {
        announcementRepository.deleteById(id);
        log.info("Announcement {} deleted", id);
    }

    @Transactional(readOnly = true)
    public List<AnnouncementDto> getAllAnnouncements() {
        return announcementRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AnnouncementStatsDto getAnnouncementStatistics() {
        long total = announcementRepository.count();
        long active = announcementRepository.countActiveAnnouncements();
        long pinned = announcementRepository.countPinnedAnnouncements();
        long urgent = announcementRepository.countUrgentAnnouncements();

        AnnouncementStatsDto stats = new AnnouncementStatsDto();
        stats.setTotal(total);
        stats.setTotalActive(active);
        stats.setPinned(pinned);
        stats.setTotalUrgent(urgent);

        return stats;
    }

    private AnnouncementDto convertToDto(Announcement announcement) {
        AnnouncementDto dto = new AnnouncementDto();
        dto.setId(announcement.getId());

        if (announcement.getAuthor() != null) {
            dto.setAuthorId(announcement.getAuthor().getId());
            dto.setAuthorName(announcement.getAuthor().getFirstName() + " " + announcement.getAuthor().getLastName());
        }

        dto.setTitle(announcement.getTitle());
        dto.setContent(announcement.getContent());
        dto.setType(announcement.getType() != null ? announcement.getType().name() : null);
        dto.setPriority(announcement.getPriority() != null ? announcement.getPriority().name() : null);
        dto.setIsActive(announcement.getIsActive());
        dto.setIsUrgent(announcement.getIsUrgent());
        dto.setIsPinned(announcement.getIsPinned());
        dto.setTargetAudience(announcement.getTargetAudience());
        dto.setTargetRooms(announcement.getTargetRooms());
        dto.setTargetFloors(announcement.getTargetFloors());
        dto.setPublishedAt(announcement.getPublishedAt());
        dto.setExpiresAt(announcement.getExpiresAt());
        dto.setScheduledFor(announcement.getScheduledFor());
        dto.setCreatedAt(announcement.getCreatedAt());
        dto.setUpdatedAt(announcement.getUpdatedAt());
        dto.setViewCount(announcement.getViewCount());
        dto.setAcknowledgmentCount(announcement.getAcknowledgmentCount());
        dto.setAcknowledgmentRequired(announcement.getAcknowledgmentRequired());
        dto.setImageUrl(announcement.getImageUrl());
        dto.setExternalLink(announcement.getExternalLink());
        dto.setAttachments(announcement.getAttachments());
        dto.setCategory(announcement.getCategory());
        dto.setTags(announcement.getTags());
        dto.setLanguage(announcement.getLanguage());
        dto.setStatusDisplay(announcement.getStatusDisplay());

        return dto;
    }

    @lombok.Data
    public static class AnnouncementStatsDto {
        private long total;
        private long totalActive;
        private long pinned;
        private long totalUrgent;
    }
}