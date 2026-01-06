package com.dorm.manag.service;

import com.dorm.manag.dto.DocumentDto;
import com.dorm.manag.entity.Document;
import com.dorm.manag.entity.DocumentType;
import com.dorm.manag.entity.User;
import com.dorm.manag.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final String uploadDirectory = "uploads/documents/";

    @Transactional(readOnly = true)
    public List<DocumentDto> getDocumentsForUser(User user) {
        List<Document> documents = documentRepository.findActiveDocumentsForUser();

        return documents.stream()
                .filter(doc -> doc.canAccess(user))
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DocumentDto> getDocumentsByType(DocumentType type, User user) {
        List<Document> documents = documentRepository.findByDocumentTypeAndIsActiveOrderByCreatedAtDesc(type, true);

        return documents.stream()
                .filter(doc -> doc.canAccess(user))
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DocumentDto> getFeaturedDocuments(User user) {
        List<Document> documents = documentRepository.findByIsFeaturedAndIsActiveOrderByCreatedAtDesc(true, true);

        return documents.stream()
                .filter(doc -> doc.canAccess(user))
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<DocumentDto> getDocumentById(Long id, User user) {
        return documentRepository.findById(id)
                .filter(doc -> doc.canAccess(user))
                .map(doc -> {
                    doc.incrementViewCount();
                    documentRepository.save(doc);
                    return convertToDto(doc);
                });
    }

    @Transactional
    public DocumentDto uploadDocument(MultipartFile file, String title, String description,
            DocumentType type, String accessLevel, User uploadedBy) throws IOException {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        Path uploadPath = Paths.get(uploadDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath);

        Document document = new Document();
        document.setTitle(title);
        document.setDescription(description);
        document.setDocumentType(type);
        document.setFileName(originalFilename);
        document.setFilePath(filePath.toString());
        document.setFileSize(file.getSize());
        document.setMimeType(file.getContentType());
        document.setAccessLevel(accessLevel);
        document.setUploadedBy(uploadedBy);
        document.setIsActive(true);

        if (!type.requiresApproval()) {
            document.approve(uploadedBy);
        }

        Document savedDocument = documentRepository.save(document);
        log.info("Document uploaded: {} by {}", title, uploadedBy.getUsername());

        return convertToDto(savedDocument);
    }

    @Transactional
    public DocumentDto createTextDocument(String title, String content, String description,
            DocumentType type, String accessLevel, User uploadedBy) {

        Document document = new Document();
        document.setTitle(title);
        document.setContent(content);
        document.setDescription(description);
        document.setDocumentType(type);
        document.setAccessLevel(accessLevel);
        document.setUploadedBy(uploadedBy);
        document.setIsActive(true);

        if (!type.requiresApproval()) {
            document.approve(uploadedBy);
        }

        Document savedDocument = documentRepository.save(document);
        log.info("Text document created: {} by {}", title, uploadedBy.getUsername());

        return convertToDto(savedDocument);
    }

    @Transactional
    public DocumentDto updateDocument(Long id, String title, String description,
            DocumentType type, String accessLevel, User updatedBy) {

        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        document.setTitle(title);
        document.setDescription(description);
        document.setDocumentType(type);
        document.setAccessLevel(accessLevel);

        if (type.requiresApproval()) {
            document.setApprovedBy(null);
            document.setApprovedAt(null);
        }

        Document savedDocument = documentRepository.save(document);
        log.info("Document updated: {} by {}", title, updatedBy.getUsername());

        return convertToDto(savedDocument);
    }

    @Transactional
    public DocumentDto createNewVersion(Long documentId, String newVersion, String versionNotes,
            MultipartFile file, User updatedBy) throws IOException {

        Document currentDocument = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (!currentDocument.getDocumentType().supportsVersioning()) {
            throw new IllegalArgumentException("Document type does not support versioning");
        }

        Document newVersionDoc = currentDocument.createNewVersion(newVersion, versionNotes, updatedBy);

        if (file != null && !file.isEmpty()) {
            // Save new file
            Path uploadPath = Paths.get(uploadDirectory);
            String uniqueFilename = UUID.randomUUID().toString() +
                    file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath);

            newVersionDoc.setFileName(file.getOriginalFilename());
            newVersionDoc.setFilePath(filePath.toString());
            newVersionDoc.setFileSize(file.getSize());
            newVersionDoc.setMimeType(file.getContentType());
        }

        Document savedDocument = documentRepository.save(newVersionDoc);
        documentRepository.save(currentDocument); // Save deactivated current version

        log.info("New document version created: {} v{} by {}",
                savedDocument.getTitle(), newVersion, updatedBy.getUsername());

        return convertToDto(savedDocument);
    }

    @Transactional
    public void approveDocument(Long id, User approver) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        document.approve(approver);
        documentRepository.save(document);

        log.info("Document approved: {} by {}", document.getTitle(), approver.getUsername());
    }

    @Transactional
    public void archiveDocument(Long id, User archivedBy) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        document.setIsActive(false);
        documentRepository.save(document);

        log.info("Document archived: {} by {}", document.getTitle(), archivedBy.getUsername());
    }

    @Transactional(readOnly = true)
    public byte[] downloadDocument(Long id, User user) throws IOException {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (!document.canAccess(user)) {
            throw new RuntimeException("Access denied");
        }

        if (!document.isFileDocument()) {
            throw new RuntimeException("Document is not a file");
        }

        document.incrementDownloadCount();
        documentRepository.save(document);

        Path filePath = Paths.get(document.getFilePath());
        if (!Files.exists(filePath)) {
            throw new RuntimeException("File not found on disk");
        }

        return Files.readAllBytes(filePath);
    }

    @Transactional(readOnly = true)
    public List<DocumentDto> searchDocuments(String searchTerm, User user) {
        List<Document> documents = documentRepository.searchDocuments(searchTerm);

        return documents.stream()
                .filter(doc -> doc.canAccess(user))
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DocumentDto> getDocumentsNeedingReview() {
        LocalDateTime now = LocalDateTime.now();
        List<Document> documents = documentRepository.findDocumentsNeedingReview(now);

        return documents.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DocumentStatsDto getDocumentStatistics() {
        long totalDocuments = documentRepository.countActiveDocuments();
        long pendingApproval = documentRepository.countPendingApproval();
        long featuredDocuments = documentRepository.countFeaturedDocuments();

        DocumentStatsDto stats = new DocumentStatsDto();
        stats.setTotalDocuments(totalDocuments);
        stats.setPendingApproval(pendingApproval);
        stats.setFeaturedDocuments(featuredDocuments);

        return stats;
    }

    private DocumentDto convertToDto(Document document) {
        DocumentDto dto = new DocumentDto();
        dto.setId(document.getId());
        dto.setTitle(document.getTitle());
        dto.setDescription(document.getDescription());
        dto.setDocumentType(document.getDocumentType());
        dto.setFileName(document.getFileName());
        dto.setFileSize(document.getFileSize());
        dto.setFormattedFileSize(document.getFormattedFileSize());
        dto.setMimeType(document.getMimeType());
        dto.setAccessLevel(document.getAccessLevel());
        dto.setVersion(document.getVersion());
        dto.setVersionNotes(document.getVersionNotes());
        dto.setIsActive(document.getIsActive());
        dto.setIsFeatured(document.getIsFeatured());
        dto.setRequiresAcknowledgment(document.getRequiresAcknowledgment());
        dto.setCreatedAt(document.getCreatedAt());
        dto.setUpdatedAt(document.getUpdatedAt());
        dto.setViewCount(document.getViewCount());
        dto.setDownloadCount(document.getDownloadCount());
        dto.setLanguage(document.getLanguage());
        dto.setValidFrom(document.getValidFrom());
        dto.setValidUntil(document.getValidUntil());
        dto.setCategory(document.getCategory());
        dto.setTags(document.getTags());

        if (document.getUploadedBy() != null) {
            dto.setUploadedByName(document.getUploadedBy().getFirstName() + " " +
                    document.getUploadedBy().getLastName());
        }

        if (document.getApprovedBy() != null) {
            dto.setApprovedByName(document.getApprovedBy().getFirstName() + " " +
                    document.getApprovedBy().getLastName());
            dto.setApprovedAt(document.getApprovedAt());
        }

        dto.setExpired(document.isExpired());
        dto.setNeedsReview(document.needsReview());
        dto.setApproved(document.isApproved());
        dto.setFileDocument(document.isFileDocument());
        dto.setTextDocument(document.isTextDocument());
        dto.setDaysUntilExpiry(document.getDaysUntilExpiry());
        dto.setPopular(document.isPopular());

        return dto;
    }

    @lombok.Data
    public static class DocumentStatsDto {
        private long totalDocuments;
        private long pendingApproval;
        private long featuredDocuments;
    }
}