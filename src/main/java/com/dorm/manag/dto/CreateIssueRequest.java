package com.dorm.manag.dto;

import com.dorm.manag.entity.IssueCategory;
import com.dorm.manag.entity.IssuePriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateIssueRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 100, message = "Title must be between 5 and 100 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 1000, message = "Description must be between 10 and 1000 characters")
    private String description;

    @NotNull(message = "Category is required")
    private IssueCategory category;

    private IssuePriority priority;

    @Size(max = 100, message = "Location details cannot exceed 100 characters")
    private String locationDetails;

    // Validation methods
    public boolean isValid() {
        return title != null && !title.trim().isEmpty() && title.length() >= 5 && title.length() <= 100 &&
                description != null && !description.trim().isEmpty() && description.length() >= 10
                && description.length() <= 1000 &&
                category != null;
    }

    public boolean isUrgent() {
        if (priority != null && priority.requiresImmediateAttention()) {
            return true;
        }

        if (description != null) {
            String desc = description.toLowerCase();
            return desc.contains("emergency") || desc.contains("urgent") || desc.contains("danger") ||
                    desc.contains("flooding") || desc.contains("fire") || desc.contains("gas leak") ||
                    desc.contains("no water") || desc.contains("no electricity");
        }

        return false;
    }

    public IssuePriority getEffectivePriority() {
        if (priority != null) {
            return priority;
        }
        // Auto-determine priority based on category and description
        return IssuePriority.determinePriority(category, description);
    }

    public static CreateIssueRequest plumbingIssue(String title, String description) {
        CreateIssueRequest request = new CreateIssueRequest();
        request.setTitle(title);
        request.setDescription(description);
        request.setCategory(IssueCategory.PLUMBING);
        request.setPriority(IssuePriority.HIGH);
        return request;
    }

    public static CreateIssueRequest electricalIssue(String title, String description) {
        CreateIssueRequest request = new CreateIssueRequest();
        request.setTitle(title);
        request.setDescription(description);
        request.setCategory(IssueCategory.ELECTRICAL);
        request.setPriority(IssuePriority.URGENT);
        return request;
    }

    public static CreateIssueRequest cleaningIssue(String title, String description) {
        CreateIssueRequest request = new CreateIssueRequest();
        request.setTitle(title);
        request.setDescription(description);
        request.setCategory(IssueCategory.CLEANING);
        request.setPriority(IssuePriority.LOW);
        return request;
    }

    public static CreateIssueRequest furnitureIssue(String title, String description) {
        CreateIssueRequest request = new CreateIssueRequest();
        request.setTitle(title);
        request.setDescription(description);
        request.setCategory(IssueCategory.FURNITURE);
        request.setPriority(IssuePriority.MEDIUM);
        return request;
    }

    public static CreateIssueRequest internetIssue(String title, String description) {
        CreateIssueRequest request = new CreateIssueRequest();
        request.setTitle(title);
        request.setDescription(description);
        request.setCategory(IssueCategory.INTERNET);
        request.setPriority(IssuePriority.MEDIUM);
        return request;
    }

    public static CreateIssueRequest create(String title, String description, IssueCategory category) {
        CreateIssueRequest request = new CreateIssueRequest();
        request.setTitle(title);
        request.setDescription(description);
        request.setCategory(category);
        return request;
    }

    public static CreateIssueRequest create(String title, String description, IssueCategory category,
            IssuePriority priority) {
        CreateIssueRequest request = create(title, description, category);
        request.setPriority(priority);
        return request;
    }

    public String getValidationError() {
        if (title == null || title.trim().isEmpty()) {
            return "Title is required";
        }
        if (title.length() < 5 || title.length() > 100) {
            return "Title must be between 5 and 100 characters";
        }
        if (description == null || description.trim().isEmpty()) {
            return "Description is required";
        }
        if (description.length() < 10 || description.length() > 1000) {
            return "Description must be between 10 and 1000 characters";
        }
        if (category == null) {
            return "Category is required";
        }
        if (locationDetails != null && locationDetails.length() > 100) {
            return "Location details cannot exceed 100 characters";
        }
        return null;
    }

    @Override
    public String toString() {
        return "CreateIssueRequest{" +
                "title='" + title + '\'' +
                ", category=" + category +
                ", priority=" + priority +
                ", locationDetails='" + locationDetails + '\'' +
                ", urgent=" + isUrgent() +
                '}';
    }
}