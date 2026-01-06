package com.dorm.manag.controller;

import com.dorm.manag.dto.DocumentDto;
import com.dorm.manag.entity.DocumentType;
import com.dorm.manag.entity.User;
import com.dorm.manag.service.DocumentService;
import com.dorm.manag.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class DocumentController {

    private final DocumentService documentService;
    private final UserService userService;

    // PUBLIC/USER

    @GetMapping
    public ResponseEntity<?> getDocuments(@RequestParam(required = false) DocumentType type,
            @RequestParam(defaultValue = "false") boolean featured,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<DocumentDto> documents;
            if (featured) {
                documents = documentService.getFeaturedDocuments(user);
            } else if (type != null) {
                documents = documentService.getDocumentsByType(type, user);
            } else {
                documents = documentService.getDocumentsForUser(user);
            }

            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            log.error("Error retrieving documents: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve documents");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchDocuments(@RequestParam String q, Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<DocumentDto> documents = documentService.searchDocuments(q, user);
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            log.error("Error searching documents: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to search documents");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/categories")
    public ResponseEntity<?> getDocumentCategories() {
        try {
            DocumentType[] types = DocumentType.values();
            return ResponseEntity.ok(types);
        } catch (Exception e) {
            log.error("Error retrieving document categories: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve categories");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDocumentById(@PathVariable Long id, Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Optional<DocumentDto> document = documentService.getDocumentById(id, user);
            if (document.isEmpty()) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "Document not found or access denied");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            return ResponseEntity.ok(document.get());
        } catch (Exception e) {
            log.error("Error retrieving document {}: {}", id, e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve document");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadDocument(@PathVariable Long id, Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            byte[] fileContent = documentService.downloadDocument(id, user);
            Optional<DocumentDto> documentOpt = documentService.getDocumentById(id, user);

            if (documentOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            DocumentDto document = documentOpt.get();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(document.getMimeType()));
            headers.setContentDispositionFormData("attachment", document.getFileName());
            headers.setContentLength(fileContent.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileContent);
        } catch (Exception e) {
            log.error("Error downloading document {}: {}", id, e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to download document");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // ADMIN

    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> uploadDocument(@RequestParam(required = false) MultipartFile file,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String content,
            @RequestParam DocumentType type,
            @RequestParam String accessLevel,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            DocumentDto document;
            if (file != null && !file.isEmpty()) {
                document = documentService.uploadDocument(file, title, description, type, accessLevel, user);
            } else if (content != null) {
                document = documentService.createTextDocument(title, content, description, type, accessLevel, user);
            } else {
                throw new IllegalArgumentException("Either file or content must be provided");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Document uploaded successfully");
            response.put("document", document);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error uploading document: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to upload document");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateDocument(@PathVariable Long id,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam DocumentType type,
            @RequestParam String accessLevel,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            DocumentDto document = documentService.updateDocument(id, title, description, type, accessLevel, user);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Document updated successfully");
            response.put("document", document);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating document {}: {}", id, e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to update document");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PostMapping("/{id}/new-version")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createNewVersion(@PathVariable Long id,
            @RequestParam String version,
            @RequestParam String versionNotes,
            @RequestParam(required = false) MultipartFile file,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            DocumentDto newVersion = documentService.createNewVersion(id, version, versionNotes, file, user);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "New document version created successfully");
            response.put("document", newVersion);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating new version for document {}: {}", id, e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to create new version");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approveDocument(@PathVariable Long id, Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            documentService.approveDocument(id, user);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Document approved successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error approving document {}: {}", id, e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to approve document");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> archiveDocument(@PathVariable Long id, Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            documentService.archiveDocument(id, user);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Document archived successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error archiving document {}: {}", id, e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to archive document");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getDocumentStatistics() {
        try {
            DocumentService.DocumentStatsDto stats = documentService.getDocumentStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error retrieving document statistics: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve statistics");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}