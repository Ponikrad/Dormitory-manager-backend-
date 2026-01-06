package com.dorm.manag.dto;

import com.dorm.manag.entity.KeyAssignment;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class KeyAssignmentDto {
    private Long id;
    private Long keyId;
    private String keyCode;
    private String keyDescription;
    private Long userId;
    private String userName;
    private KeyAssignment.AssignmentType assignmentType;
    private KeyAssignment.AssignmentStatus status;
    private LocalDateTime issuedAt;
    private LocalDateTime expectedReturn;
    private LocalDateTime returnedAt;
    private BigDecimal depositAmount;
    private Boolean depositPaid;
    private BigDecimal fineAmount;
    private BigDecimal replacementCost;
    private String issueNotes;
    private String returnNotes;
    private String conditionOnIssue;
    private String conditionOnReturn;
    private String issuedByName;
    private String returnedToName;

    private Boolean active;
    private Boolean overdueNow;
    private Long hoursOverdue;
    private Long daysOverdue;
    private BigDecimal totalAmountOwed;
    private String assignmentSummary;
}
