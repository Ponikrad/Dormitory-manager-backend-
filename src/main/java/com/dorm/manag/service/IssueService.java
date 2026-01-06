package com.dorm.manag.service;

import com.dorm.manag.dto.CreateIssueRequest;
import com.dorm.manag.dto.IssueDto;
import com.dorm.manag.entity.*;
import com.dorm.manag.repository.IssueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IssueService {

    private final IssueRepository issueRepository;
    private final UserService userService;

    @Transactional
    public IssueDto reportIssue(CreateIssueRequest request, User user) {
        log.info("Reporting new issue for user: {} - {}", user.getUsername(), request.getTitle());

        validateIssueRequest(request);

        Issue issue = new Issue();
        issue.setUser(user);
        issue.setTitle(request.getTitle());
        issue.setDescription(request.getDescription());
        issue.setCategory(request.getCategory());
        issue.setPriority(request.getEffectivePriority());
        issue.setLocationDetails(request.getLocationDetails());
        issue.setRoomNumber(user.getRoomNumber());
        issue.setStatus(IssueStatus.REPORTED);
        issue.setContractorRequired(request.getCategory().requiresExternalContractor());
        issue.setReopenedCount(0);

        Issue savedIssue = issueRepository.save(issue);
        log.info("Issue reported with ID: {} - Priority: {}", savedIssue.getId(), savedIssue.getPriority());

        if (savedIssue.requiresUrgentAttention()) {
            log.warn("URGENT ISSUE REPORTED - ID: {} - {}", savedIssue.getId(), savedIssue.getTitle());
        }

        return convertToDto(savedIssue);
    }

    @Transactional(readOnly = true)
    public List<IssueDto> getUserIssues(User user) {
        List<Issue> issues = issueRepository.findByUserOrderByReportedAtDesc(user);
        return issues.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IssueDto> getUserIssues(User user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Issue> issues = issueRepository.findByUserOrderByReportedAtDesc(user, pageable);

        return issues.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<IssueDto> getIssueById(Long id) {
        return issueRepository.findById(id)
                .map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public List<IssueDto> getAllIssues() {
        List<Issue> issues = issueRepository.findAll();
        return issues.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IssueDto> getOpenIssues() {
        List<Issue> openIssues = issueRepository.findOpenIssuesOrderByPriorityAndDate();
        return openIssues.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IssueDto> getUrgentIssues() {
        List<Issue> urgentIssues = issueRepository.findUrgentOpenIssues();
        return urgentIssues.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IssueDto> getOverdueIssues() {
        List<Issue> overdueIssues = issueRepository.findOverdueIssues(LocalDateTime.now());
        return overdueIssues.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IssueDto> getIssuesByCategory(IssueCategory category) {
        List<Issue> issues = issueRepository.findByCategoryOrderByReportedAtDesc(category);
        return issues.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IssueDto> getIssuesByStatus(IssueStatus status) {
        List<Issue> issues = issueRepository.findByStatusOrderByReportedAtDesc(status);
        return issues.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IssueDto> getAssignedIssues(User assignedUser) {
        List<Issue> issues = issueRepository.findByAssignedToOrderByReportedAtDesc(assignedUser);
        return issues.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public IssueDto updateIssueStatus(Long issueId, IssueStatus newStatus, String adminNotes) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found with id: " + issueId));

        IssueStatus oldStatus = issue.getStatus();

        if (!canTransitionStatus(oldStatus, newStatus)) {
            throw new IllegalArgumentException("Invalid status transition from " + oldStatus + " to " + newStatus);
        }

        issue.setStatus(newStatus);

        switch (newStatus) {
            case ACKNOWLEDGED -> {
                if (issue.getAcknowledgedAt() == null) {
                    issue.acknowledge();
                }
            }
            case IN_PROGRESS -> issue.startProgress();
            case RESOLVED -> {
                if (adminNotes != null) {
                    issue.resolve(adminNotes);
                } else {
                    issue.resolve("Issue resolved by admin");
                }
            }
            case CANCELLED -> issue.cancel();
            case ESCALATED -> issue.escalate();
        }

        if (adminNotes != null) {
            issue.setAdminNotes(adminNotes);
        }

        Issue updatedIssue = issueRepository.save(issue);

        log.info("Issue {} status updated from {} to {} by admin",
                issueId, oldStatus, newStatus);

        return convertToDto(updatedIssue);
    }

    @Transactional
    public IssueDto assignIssue(Long issueId, Long assignedToUserId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found with id: " + issueId));

        User assignedUser = userService.findById(assignedToUserId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + assignedToUserId));

        if (!assignedUser.getRole().hasReceptionistPrivileges()) {
            throw new IllegalArgumentException("User does not have permission to be assigned issues");
        }

        issue.assignTo(assignedUser);
        Issue assignedIssue = issueRepository.save(issue);

        log.info("Issue {} assigned to user {} ({})",
                issueId, assignedUser.getUsername(), assignedUser.getFullName());

        return convertToDto(assignedIssue);
    }

    @Transactional
    public IssueDto reopenIssue(Long issueId, User user) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found with id: " + issueId));

        if (!issue.getUser().getId().equals(user.getId()) && !user.getRole().hasAdminPrivileges()) {
            throw new IllegalArgumentException("You can only reopen your own issues");
        }

        if (!issue.canBeReopened()) {
            throw new IllegalArgumentException("Issue cannot be reopened");
        }

        issue.reopen();
        Issue reopenedIssue = issueRepository.save(issue);

        log.info("Issue {} reopened by user {}", issueId, user.getUsername());

        return convertToDto(reopenedIssue);
    }

    @Transactional
    public IssueDto addResolutionNotes(Long issueId, String resolutionNotes) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found with id: " + issueId));

        issue.setResolutionNotes(resolutionNotes);
        Issue updatedIssue = issueRepository.save(issue);

        return convertToDto(updatedIssue);
    }

    @Transactional
    public IssueDto rateIssueResolution(Long issueId, int rating, User user) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found with id: " + issueId));

        if (!issue.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You can only rate your own issues");
        }

        if (!issue.isResolved()) {
            throw new IllegalArgumentException("Issue must be resolved before rating");
        }

        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        issue.setUserSatisfactionRating(rating);
        Issue ratedIssue = issueRepository.save(issue);

        log.info("Issue {} rated {} stars by user {}", issueId, rating, user.getUsername());

        return convertToDto(ratedIssue);
    }

    @Transactional(readOnly = true)
    public IssueStatsDto getIssueStatistics() {
        long totalReported = issueRepository.countReportedIssues();
        long totalInProgress = issueRepository.countInProgressIssues();
        long totalResolved = issueRepository.countResolvedIssues();
        long totalUrgent = issueRepository.countUrgentOpenIssues();
        long totalOverdue = issueRepository.countOverdueIssues(LocalDateTime.now());

        Double avgSatisfaction = issueRepository.getAverageUserSatisfactionRating();

        IssueStatsDto stats = new IssueStatsDto();
        stats.setTotalReported(totalReported);
        stats.setTotalInProgress(totalInProgress);
        stats.setTotalResolved(totalResolved);
        stats.setTotalUrgent(totalUrgent);
        stats.setTotalOverdue(totalOverdue);
        stats.setAverageSatisfactionRating(avgSatisfaction != null ? avgSatisfaction : 0.0);

        return stats;
    }

    @Transactional(readOnly = true)
    public List<IssueDto> getUnacknowledgedIssues(int hoursThreshold) {
        LocalDateTime thresholdDate = LocalDateTime.now().minusHours(hoursThreshold);
        List<Issue> unacknowledgedIssues = issueRepository.findUnacknowledgedIssues(thresholdDate);

        return unacknowledgedIssues.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IssueDto> findIssuesByCriteria(IssueCategory category, IssueStatus status,
            IssuePriority priority, Long assignedToId) {
        List<Issue> issues = issueRepository.findByMultipleCriteria(category, status, priority, assignedToId);

        return issues.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private boolean canTransitionStatus(IssueStatus from, IssueStatus to) {
        IssueStatus[] allowedTransitions = from.getNextPossibleStatuses();
        for (IssueStatus allowed : allowedTransitions) {
            if (allowed == to)
                return true;
        }
        return false;
    }

    private void validateIssueRequest(CreateIssueRequest request) {
        if (!request.isValid()) {
            String error = request.getValidationError();
            throw new IllegalArgumentException("Invalid issue request: " + error);
        }
    }

    private IssueDto convertToDto(Issue issue) {
        IssueDto dto = new IssueDto();
        dto.setId(issue.getId());
        dto.setUserId(issue.getUser().getId());
        dto.setUserFullName(issue.getUser().getFirstName() + " " + issue.getUser().getLastName());
        dto.setUserEmail(issue.getUser().getEmail());
        dto.setTitle(issue.getTitle());
        dto.setDescription(issue.getDescription());
        dto.setCategory(issue.getCategory());
        dto.setStatus(issue.getStatus());
        dto.setPriority(issue.getPriority());
        dto.setRoomNumber(issue.getRoomNumber());
        dto.setLocationDetails(issue.getLocationDetails());
        dto.setReportedAt(issue.getReportedAt());
        dto.setAcknowledgedAt(issue.getAcknowledgedAt());
        dto.setResolvedAt(issue.getResolvedAt());
        dto.setEstimatedResolution(issue.getEstimatedResolution());
        dto.setAdminNotes(issue.getAdminNotes());

        if (issue.getAssignedTo() != null) {
            dto.setAssignedToUserId(issue.getAssignedTo().getId());
            dto.setAssignedToUserName(issue.getAssignedTo().getFirstName() + " " + issue.getAssignedTo().getLastName());
        }

        dto.setContractorRequired(issue.getContractorRequired());
        dto.setCostEstimate(issue.getCostEstimate());
        dto.setResolutionNotes(issue.getResolutionNotes());
        dto.setUserSatisfactionRating(issue.getUserSatisfactionRating());
        dto.setReopenedCount(issue.getReopenedCount());
        dto.setActualResolutionTimeHours(issue.getActualResolutionTimeHours());

        dto.calculateFields();
        return dto;
    }

    @lombok.Data
    public static class IssueStatsDto {
        private long totalReported;
        private long totalInProgress;
        private long totalResolved;
        private long totalUrgent;
        private long totalOverdue;
        private double averageSatisfactionRating;
    }
}