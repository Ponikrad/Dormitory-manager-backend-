package com.dorm.manag.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private DocumentType documentType;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "file_url")
    private String fileUrl; // For external documents or cloud storage

    @Column(name = "file_size")
    private Long fileSize; // in bytes

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content; // For text documents stored in database

    // Access control
    @Column(name = "access_level")
    private String accessLevel = "STUDENTS"; // PUBLIC, STUDENTS, STAFF, ADMIN

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_featured", nullable = false)
    private Boolean isFeatured = false;

    @Column(name = "requires_acknowledgment", nullable = false)
    private Boolean requiresAcknowledgment = false;

    // Versioning
    @Column(name = "version")
    private String version = "1.0";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_version_id")
    private Document previousVersion;

    @Column(name = "version_notes")
    private String versionNotes;

    // Metadata
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id", nullable = false)
    private User uploadedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private User approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Usage tracking
    @Column(name = "download_count")
    private Long downloadCount = 0L;

    @Column(name = "view_count")
    private Long viewCount = 0L;

    @Column(name = "last_accessed")
    private LocalDateTime lastAccessed;

    // Categorization
    @Column(name = "category")
    private String category;

    @Column(name = "tags")
    private String tags; // Comma-separated tags

    @Column(name = "language")
    private String language = "PL"; // PL, EN

    // Validity
    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(name = "review_date")
    private LocalDateTime reviewDate;

    // Constructors
    public Document(String title, DocumentType documentType, User uploadedBy) {
        this.title = title;
        this.documentType = documentType;
        this.uploadedBy = uploadedBy;
        this.accessLevel = documentType.getDefaultAccessLevel();
        this.isActive = true;
        this.version = "1.0";
    }

    public Document(String title, String content, DocumentType documentType, User uploadedBy) {
        this(title, documentType, uploadedBy);
        this.content = content;
    }

    // Helper methods
    public boolean isPublic() {
        return "PUBLIC".equals(accessLevel);
    }

    public boolean isForStudents() {
        return "STUDENTS".equals(accessLevel) || "PUBLIC".equals(accessLevel);
    }

    public boolean isForStaff() {
        return "STAFF".equals(accessLevel) || "ADMIN".equals(accessLevel) || "PUBLIC".equals(accessLevel);
    }

    public boolean canAccess(User user) {
        return switch (accessLevel) {
            case "PUBLIC" -> true;
            case "STUDENTS" -> user.getRole() == Role.STUDENT || user.getRole().hasReceptionistPrivileges();
            case "STAFF" -> user.getRole().hasReceptionistPrivileges();
            case "ADMIN" -> user.getRole().hasAdminPrivileges();
            default -> false;
        };
    }

    public boolean isExpired() {
        return validUntil != null && LocalDateTime.now().isAfter(validUntil);
    }

    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return isActive &&
                (validFrom == null || now.isAfter(validFrom)) &&
                (validUntil == null || now.isBefore(validUntil));
    }

    public boolean needsReview() {
        return reviewDate != null && LocalDateTime.now().isAfter(reviewDate);
    }

    public boolean isFileDocument() {
        return filePath != null || fileUrl != null;
    }

    public boolean isTextDocument() {
        return content != null && !content.trim().isEmpty();
    }

    public void incrementDownloadCount() {
        this.downloadCount = (this.downloadCount == null ? 0L : this.downloadCount) + 1;
        this.lastAccessed = LocalDateTime.now();
    }

    public void incrementViewCount() {
        this.viewCount = (this.viewCount == null ? 0L : this.viewCount) + 1;
        this.lastAccessed = LocalDateTime.now();
    }

    public void approve(User approver) {
        this.approvedBy = approver;
        this.approvedAt = LocalDateTime.now();
    }

    public boolean isApproved() {
        return approvedBy != null && approvedAt != null;
    }

    public boolean requiresApproval() {
        return documentType.requiresApproval();
    }

    public Document createNewVersion(String newVersion, String notes, User updatedBy) {
        Document newDoc = new Document();
        newDoc.setTitle(this.title);
        newDoc.setDescription(this.description);
        newDoc.setDocumentType(this.documentType);
        newDoc.setAccessLevel(this.accessLevel);
        newDoc.setCategory(this.category);
        newDoc.setLanguage(this.language);
        newDoc.setUploadedBy(updatedBy);
        newDoc.setVersion(newVersion);
        newDoc.setVersionNotes(notes);
        newDoc.setPreviousVersion(this);

        // Deactivate current version
        this.setIsActive(false);

        return newDoc;
    }

    public String getFileExtension() {
        if (fileName == null)
            return null;
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(dotIndex + 1).toLowerCase() : null;
    }

    public String getFormattedFileSize() {
        if (fileSize == null)
            return "Unknown";

        double size = fileSize.doubleValue();
        String[] units = { "B", "KB", "MB", "GB" };
        int unitIndex = 0;

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        return String.format("%.1f %s", size, units[unitIndex]);
    }

    public long getDaysUntilExpiry() {
        if (validUntil == null)
            return Long.MAX_VALUE;
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), validUntil);
    }

    public boolean isPopular() {
        return downloadCount != null && downloadCount > 10 || viewCount != null && viewCount > 50;
    }

    @Override
    public String toString() {
        return "Document{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", documentType=" + documentType +
                ", version='" + version + '\'' +
                ", accessLevel='" + accessLevel + '\'' +
                ", isActive=" + isActive +
                ", fileName='" + fileName + '\'' +
                ", fileSize=" + fileSize +
                ", downloadCount=" + downloadCount +
                ", viewCount=" + viewCount +
                '}';
    }
}