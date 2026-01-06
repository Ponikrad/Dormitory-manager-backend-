package com.dorm.manag.dto;

import com.dorm.manag.entity.ApplicationStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ApplicationDto {
    private Long id;
    private String applicationNumber;

    // Personal
    private String firstName;
    private String lastName;
    private String fullName;
    private LocalDate dateOfBirth;
    private String gender;
    private String nationality;
    private String email;
    private String phoneNumber;

    // Academic
    private String universityName;
    private String fieldOfStudy;
    private Integer yearOfStudy;
    private Boolean isExchangeStudent;

    // Preferences
    private String preferredRoomType;
    private Integer preferredFloor;

    // Stay period
    private LocalDate moveInDate;
    private LocalDate moveOutDate;
    private Integer durationMonths;

    // Status & process
    private ApplicationStatus status;
    private Integer priorityScore;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime approvedAt;

    // Room
    private String assignedRoomNumber;
    private LocalDateTime roomAssignedAt;

    // Payments
    private Boolean applicationFeePaid;
    private Boolean depositPaid;
    private BigDecimal totalAmountDue;

    // Notes
    private String applicantNotes;
    private String adminNotes;
    private String rejectionReason;
    private LocalDateTime responseDeadline;

    // Relations (flattened to names for simplicity)
    private String reviewedByName;
    private String approvedByName;

    // Derived/extra fields
    private boolean approved;
    private boolean pending;
    private boolean isFinal;
    private boolean canBeModified;
    private boolean overdue;
    private Integer age;
    private Long daysSinceSubmission;
    private Long daysUntilDeadline;
    private boolean paymentComplete;
}
