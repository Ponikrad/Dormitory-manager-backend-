package com.dorm.manag.repository;

import com.dorm.manag.entity.Document;
import com.dorm.manag.entity.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByDocumentTypeAndIsActiveOrderByCreatedAtDesc(DocumentType documentType, Boolean isActive);

    List<Document> findByIsFeaturedAndIsActiveOrderByCreatedAtDesc(Boolean isFeatured, Boolean isActive);

    @Query("SELECT d FROM Document d WHERE d.isActive = true ORDER BY d.createdAt DESC")
    List<Document> findActiveDocumentsForUser();

    @Query("SELECT d FROM Document d WHERE (d.title LIKE %:searchTerm% OR d.description LIKE %:searchTerm%) AND d.isActive = true")
    List<Document> searchDocuments(@Param("searchTerm") String searchTerm);

    @Query("SELECT d FROM Document d WHERE d.reviewDate < :currentDate AND d.isActive = true")
    List<Document> findDocumentsNeedingReview(@Param("currentDate") LocalDateTime currentDate);

    @Query("SELECT COUNT(d) FROM Document d WHERE d.isActive = true")
    long countActiveDocuments();

    @Query("SELECT COUNT(d) FROM Document d WHERE d.approvedBy IS NULL AND d.isActive = true")
    long countPendingApproval();

    @Query("SELECT COUNT(d) FROM Document d WHERE d.isFeatured = true AND d.isActive = true")
    long countFeaturedDocuments();
}