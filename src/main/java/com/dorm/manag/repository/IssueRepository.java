package com.dorm.manag.repository;

import com.dorm.manag.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Long> {

    // Find by user
    List<Issue> findByUserOrderByReportedAtDesc(User user);

    Page<Issue> findByUserOrderByReportedAtDesc(User user, Pageable pageable);

    List<Issue> findByUserId(Long userId);

    // Find by status
    List<Issue> findByStatus(IssueStatus status);

    List<Issue> findByStatusOrderByReportedAtDesc(IssueStatus status);

    // Find by category
    List<Issue> findByCategory(IssueCategory category);

    List<Issue> findByCategoryOrderByReportedAtDesc(IssueCategory category);

    // Find by priority
    List<Issue> findByPriority(IssuePriority priority);

    List<Issue> findByPriorityOrderByReportedAtDesc(IssuePriority priority);

    // Find by room
    List<Issue> findByRoomNumberOrderByReportedAtDesc(String roomNumber);

    // Find by assigned user
    List<Issue> findByAssignedToOrderByReportedAtDesc(User assignedTo);

    List<Issue> findByAssignedToId(Long assignedToId);

    // Find open issues
    @Query("SELECT i FROM Issue i WHERE i.status NOT IN ('RESOLVED', 'CANCELLED', 'CLOSED') ORDER BY i.priority DESC, i.reportedAt ASC")
    List<Issue> findOpenIssuesOrderByPriorityAndDate();

    // Find overdue issues
    @Query("SELECT i FROM Issue i WHERE i.estimatedResolution < :currentDate AND i.status NOT IN ('RESOLVED', 'CANCELLED', 'CLOSED')")
    List<Issue> findOverdueIssues(@Param("currentDate") LocalDateTime currentDate);

    // Find urgent issues
    @Query("SELECT i FROM Issue i WHERE i.priority IN ('URGENT', 'CRITICAL') AND i.status NOT IN ('RESOLVED', 'CANCELLED', 'CLOSED') ORDER BY i.reportedAt ASC")
    List<Issue> findUrgentOpenIssues();

    // Find issues by date range
    @Query("SELECT i FROM Issue i WHERE i.reportedAt BETWEEN :startDate AND :endDate")
    List<Issue> findByReportedAtBetween(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Find issues reported in last N days
    @Query("SELECT i FROM Issue i WHERE i.reportedAt >= :since ORDER BY i.reportedAt DESC")
    List<Issue> findRecentIssues(@Param("since") LocalDateTime since);

    // Find issues by user and status
    @Query("SELECT i FROM Issue i WHERE i.user.id = :userId AND i.status = :status")
    List<Issue> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") IssueStatus status);

    // Find issues by category and status
    @Query("SELECT i FROM Issue i WHERE i.category = :category AND i.status = :status")
    List<Issue> findByCategoryAndStatus(@Param("category") IssueCategory category, @Param("status") IssueStatus status);

    // Statistics queries
    @Query("SELECT COUNT(i) FROM Issue i WHERE i.status = 'REPORTED'")
    long countReportedIssues();

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.status = 'IN_PROGRESS'")
    long countInProgressIssues();

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.status = 'RESOLVED'")
    long countResolvedIssues();

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.priority IN ('URGENT', 'CRITICAL') AND i.status NOT IN ('RESOLVED', 'CANCELLED', 'CLOSED')")
    long countUrgentOpenIssues();

    @Query("SELECT COUNT(i) FROM Issue i WHERE i.estimatedResolution < :currentDate AND i.status NOT IN ('RESOLVED', 'CANCELLED', 'CLOSED')")
    long countOverdueIssues(@Param("currentDate") LocalDateTime currentDate);

    // Category statistics
    @Query("SELECT i.category, COUNT(i) FROM Issue i GROUP BY i.category ORDER BY COUNT(i) DESC")
    List<Object[]> getCategoryStatistics();

    @Query("SELECT i.category, COUNT(i) FROM Issue i WHERE i.reportedAt BETWEEN :startDate AND :endDate GROUP BY i.category")
    List<Object[]> getCategoryStatisticsBetween(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Priority statistics
    @Query("SELECT i.priority, COUNT(i) FROM Issue i GROUP BY i.priority ORDER BY i.priority DESC")
    List<Object[]> getPriorityStatistics();

    // Monthly resolution statistics
    @Query("SELECT YEAR(i.resolvedAt), MONTH(i.resolvedAt), COUNT(i) FROM Issue i " +
            "WHERE i.status = 'RESOLVED' GROUP BY YEAR(i.resolvedAt), MONTH(i.resolvedAt) " +
            "ORDER BY YEAR(i.resolvedAt) DESC, MONTH(i.resolvedAt) DESC")
    List<Object[]> getMonthlyResolutionStatistics();

    // Average resolution time by category
    @Query("SELECT i.category, AVG(i.actualResolutionTimeHours) FROM Issue i " +
            "WHERE i.status = 'RESOLVED' AND i.actualResolutionTimeHours IS NOT NULL " +
            "GROUP BY i.category")
    List<Object[]> getAverageResolutionTimeByCategory();

    // Find issues needing attention (reported > X hours ago without acknowledgment)
    @Query("SELECT i FROM Issue i WHERE i.status = 'REPORTED' AND i.reportedAt < :thresholdDate ORDER BY i.priority DESC, i.reportedAt ASC")
    List<Issue> findUnacknowledgedIssues(@Param("thresholdDate") LocalDateTime thresholdDate);

    // Find issues by multiple criteria
    @Query("SELECT i FROM Issue i WHERE " +
            "(:category IS NULL OR i.category = :category) AND " +
            "(:status IS NULL OR i.status = :status) AND " +
            "(:priority IS NULL OR i.priority = :priority) AND " +
            "(:assignedToId IS NULL OR i.assignedTo.id = :assignedToId) " +
            "ORDER BY i.priority DESC, i.reportedAt ASC")
    List<Issue> findByMultipleCriteria(@Param("category") IssueCategory category,
            @Param("status") IssueStatus status,
            @Param("priority") IssuePriority priority,
            @Param("assignedToId") Long assignedToId);

    // Find issues requiring contractor
    @Query("SELECT i FROM Issue i WHERE i.contractorRequired = true AND i.status NOT IN ('RESOLVED', 'CANCELLED', 'CLOSED')")
    List<Issue> findIssuesRequiringContractor();

    // User satisfaction queries
    @Query("SELECT AVG(i.userSatisfactionRating) FROM Issue i WHERE i.userSatisfactionRating IS NOT NULL")
    Double getAverageUserSatisfactionRating();

    @Query("SELECT i.assignedTo, AVG(i.userSatisfactionRating) FROM Issue i " +
            "WHERE i.userSatisfactionRating IS NOT NULL AND i.assignedTo IS NOT NULL " +
            "GROUP BY i.assignedTo")
    List<Object[]> getAverageRatingByAssignedUser();
}