package com.dorm.manag.repository;

import com.dorm.manag.entity.Announcement;
import com.dorm.manag.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    // Find published announcements
    @Query("SELECT a FROM Announcement a LEFT JOIN FETCH a.author WHERE a.isActive = true AND " +
            "(a.publishedAt IS NULL OR a.publishedAt <= :now) AND " +
            "(a.expiresAt IS NULL OR a.expiresAt > :now) AND " +
            "(a.scheduledFor IS NULL OR a.scheduledFor <= :now) " +
            "ORDER BY a.isPinned DESC, a.priority DESC, a.publishedAt DESC")
    List<Announcement> findPublishedAnnouncements(@Param("now") LocalDateTime now);

    // Find pinned announcements
    @Query("SELECT a FROM Announcement a LEFT JOIN FETCH a.author WHERE a.isPinned = true AND a.isActive = true ORDER BY a.priority DESC, a.publishedAt DESC")
    List<Announcement> findPinnedAnnouncements();

    // Find by type
    @Query("SELECT a FROM Announcement a LEFT JOIN FETCH a.author WHERE a.type = :type ORDER BY a.publishedAt DESC")
    List<Announcement> findByTypeOrderByPublishedAtDesc(@Param("type") Announcement.AnnouncementType type);

    // Find by priority
    @Query("SELECT a FROM Announcement a LEFT JOIN FETCH a.author WHERE a.priority = :priority ORDER BY a.publishedAt DESC")
    List<Announcement> findByPriorityOrderByPublishedAtDesc(@Param("priority") Announcement.AnnouncementPriority priority);

    // Find urgent announcements
    @Query("SELECT a FROM Announcement a LEFT JOIN FETCH a.author WHERE a.isUrgent = :isUrgent AND a.isActive = :isActive ORDER BY a.publishedAt DESC")
    List<Announcement> findByIsUrgentAndIsActiveOrderByPublishedAtDesc(@Param("isUrgent") Boolean isUrgent, @Param("isActive") Boolean isActive);

    // Find by author
    @Query("SELECT a FROM Announcement a LEFT JOIN FETCH a.author WHERE a.author = :author ORDER BY a.createdAt DESC")
    List<Announcement> findByAuthorOrderByCreatedAtDesc(@Param("author") User author);

    // Find scheduled announcements
    @Query("SELECT a FROM Announcement a LEFT JOIN FETCH a.author WHERE a.scheduledFor > :now AND a.isActive = true ORDER BY a.scheduledFor ASC")
    List<Announcement> findScheduledAnnouncements(@Param("now") LocalDateTime now);

    // Find expired announcements
    @Query("SELECT a FROM Announcement a WHERE a.expiresAt < :now ORDER BY a.expiresAt DESC")
    List<Announcement> findExpiredAnnouncements(@Param("now") LocalDateTime now);

    // Find by target audience
    List<Announcement> findByTargetAudienceAndIsActiveOrderByPublishedAtDesc(String targetAudience, Boolean isActive);

    // Search
    @Query("SELECT a FROM Announcement a LEFT JOIN FETCH a.author WHERE (a.title LIKE %:searchTerm% OR a.content LIKE %:searchTerm%) AND a.isActive = true ORDER BY a.publishedAt DESC")
    List<Announcement> searchAnnouncements(@Param("searchTerm") String searchTerm);

    // Find by ID with author
    @Query("SELECT a FROM Announcement a LEFT JOIN FETCH a.author WHERE a.id = :id")
    java.util.Optional<Announcement> findByIdWithAuthor(@Param("id") Long id);

    // Statistics
    @Query("SELECT COUNT(a) FROM Announcement a WHERE a.isActive = true")
    long countActiveAnnouncements();

    @Query("SELECT COUNT(a) FROM Announcement a WHERE a.isPinned = true AND a.isActive = true")
    long countPinnedAnnouncements();

    @Query("SELECT COUNT(a) FROM Announcement a WHERE a.isUrgent = true AND a.isActive = true")
    long countUrgentAnnouncements();
}