package com.dorm.manag.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "dormitory_applications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DormitoryApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_number", unique = true, nullable = false)
    private String applicationNumber; // e.g., "APP-2024-001"

    // Personal Information
    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender")
    private String gender; // MALE, FEMALE, OTHER

    @Column(name = "nationality")
    private String nationality;

    @Column(name = "id_number")
    private String idNumber; // PESEL or passport number

    // Contact Information
    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "emergency_contact_name")
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone")
    private String emergencyContactPhone;

    // Address
    @Column(name = "street_address")
    private String streetAddress;

    @Column(name = "city")
    private String city;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "country")
    private String country;

    // Academic Information
    @Column(name = "university_name")
    private String universityName;

    @Column(name = "student_id")
    private String studentId;

    @Column(name = "field_of_study")
    private String fieldOfStudy;

    @Column(name = "year_of_study")
    private Integer yearOfStudy;

    @Column(name = "expected_graduation")
    private LocalDate expectedGraduation;

    @Column(name = "is_exchange_student", nullable = false)
    private Boolean isExchangeStudent = false;

    @Column(name = "exchange_program")
    private String exchangeProgram;

    // Accommodation Preferences
    @Column(name = "preferred_room_type")
    private String preferredRoomType; // SINGLE, DOUBLE, TRIPLE

    @Column(name = "preferred_floor")
    private Integer preferredFloor;

    @Column(name = "smoking_preference")
    private String smokingPreference; // NON_SMOKING, SMOKING_ALLOWED

    @Column(name = "special_needs", columnDefinition = "TEXT")
    private String specialNeeds;

    @Column(name = "roommate_requests")
    private String roommateRequests;

    // Stay Period
    @Column(name = "move_in_date")
    private LocalDate moveInDate;

    @Column(name = "move_out_date")
    private LocalDate moveOutDate;

    @Column(name = "duration_months")
    private Integer durationMonths;

    // Application Status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status = ApplicationStatus.SUBMITTED;

    @Column(name = "priority_score")
    private Integer priorityScore = 0; // Calculated based on criteria

    // Process Tracking
    @CreationTimestamp
    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private User approvedBy;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Room Assignment
    @Column(name = "assigned_room_number")
    private String assignedRoomNumber;

    @Column(name = "room_assigned_at")
    private LocalDateTime roomAssignedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_assigned_by_id")
    private User roomAssignedBy;

    // Financial
    @Column(name = "application_fee", precision = 8, scale = 2)
    private BigDecimal applicationFee = BigDecimal.valueOf(50); // 50 PLN application fee

    @Column(name = "application_fee_paid", nullable = false)
    private Boolean applicationFeePaid = false;

    @Column(name = "deposit_amount", precision = 8, scale = 2)
    private BigDecimal depositAmount = BigDecimal.valueOf(500); // 500 PLN deposit

    @Column(name = "deposit_paid", nullable = false)
    private Boolean depositPaid = false;

    // Documents
    @Column(name = "required_documents")
    private String requiredDocuments; // JSON array of required document types

    @Column(name = "submitted_documents")
    private String submittedDocuments; // JSON array of submitted document paths

    @Column(name = "missing_documents")
    private String missingDocuments; // JSON array of missing document types

    // Notes and Comments
    @Column(name = "applicant_notes", columnDefinition = "TEXT")
    private String applicantNotes;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // Communication
    @Column(name = "last_contact_date")
    private LocalDateTime lastContactDate;

    @Column(name = "communication_preference")
    private String communicationPreference = "EMAIL"; // EMAIL, PHONE, SMS

    @Column(name = "language_preference")
    private String languagePreference = "PL"; // PL, EN

    // Deadlines
    @Column(name = "response_deadline")
    private LocalDateTime responseDeadline;

    @Column(name = "confirmation_deadline")
    private LocalDateTime confirmationDeadline;

    // Created user account (after approval)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_user_id")
    private User createdUser;

    // Constructor
    public DormitoryApplication(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.applicationNumber = generateApplicationNumber();
        this.status = ApplicationStatus.SUBMITTED;
    }

    // Helper methods
    public boolean isApproved() {
        return status.isApproved();
    }

    public boolean isPending() {
        return status.isPending();
    }

    public boolean isFinal() {
        return status.isFinal();
    }

    public boolean canBeModified() {
        return status.canBeModified();
    }

    public boolean isOverdue() {
        return responseDeadline != null &&
                LocalDateTime.now().isAfter(responseDeadline) &&
                status.requiresStudentAction();
    }

    public void approve(User approver) {
        this.status = ApplicationStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
        this.approvedBy = approver;
        this.confirmationDeadline = LocalDateTime.now().plusDays(7);
    }

    public void reject(User reviewer, String reason) {
        this.status = ApplicationStatus.REJECTED;
        this.reviewedAt = LocalDateTime.now();
        this.reviewedBy = reviewer;
        this.rejectionReason = reason;
    }

    public void confirm() {
        if (status != ApplicationStatus.APPROVED) {
            throw new IllegalStateException("Can only confirm approved applications");
        }
        this.status = ApplicationStatus.CONFIRMED;
    }

    public void assignRoom(String roomNumber, User assignedBy) {
        if (status != ApplicationStatus.CONFIRMED) {
            throw new IllegalStateException("Can only assign rooms to confirmed applications");
        }
        this.assignedRoomNumber = roomNumber;
        this.roomAssignedAt = LocalDateTime.now();
        this.roomAssignedBy = assignedBy;
        this.status = ApplicationStatus.ROOM_ASSIGNED;
    }

    public void complete() {
        this.status = ApplicationStatus.COMPLETED;
    }

    public void withdraw() {
        if (isFinal()) {
            throw new IllegalStateException("Cannot withdraw application in final state");
        }
        this.status = ApplicationStatus.WITHDRAWN;
    }

    public void requestDocuments(String missingDocs) {
        this.status = ApplicationStatus.PENDING_DOCUMENTS;
        this.missingDocuments = missingDocs;
        this.responseDeadline = LocalDateTime.now().plusDays(14);
    }

    public long getDaysUntilDeadline() {
        if (responseDeadline == null)
            return Long.MAX_VALUE;
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), responseDeadline);
    }

    public long getDaysSinceSubmission() {
        return java.time.temporal.ChronoUnit.DAYS.between(submittedAt, LocalDateTime.now());
    }

    public int getAge() {
        if (dateOfBirth == null)
            return 0;
        return LocalDate.now().getYear() - dateOfBirth.getYear();
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean hasAllRequiredDocuments() {
        // This would need to be implemented based on document requirements
        return missingDocuments == null || missingDocuments.trim().isEmpty();
    }

    public boolean isPaymentComplete() {
        return applicationFeePaid && depositPaid;
    }

    public BigDecimal getTotalAmountDue() {
        BigDecimal total = BigDecimal.ZERO;

        if (!applicationFeePaid && applicationFee != null) {
            total = total.add(applicationFee);
        }

        if (status.isApproved() && !depositPaid && depositAmount != null) {
            total = total.add(depositAmount);
        }

        return total;
    }

    private String generateApplicationNumber() {
        int year = LocalDate.now().getYear();
        long timestamp = System.currentTimeMillis() % 100000; // Last 5 digits
        return String.format("APP-%d-%05d", year, timestamp);
    }

    public void calculatePriorityScore() {
        int score = 0;

        // Academic year bonus
        if (yearOfStudy != null) {
            score += Math.max(0, 5 - yearOfStudy); // Higher years get more points
        }

        // Exchange student bonus
        if (Boolean.TRUE.equals(isExchangeStudent)) {
            score += 10;
        }

        // Early application bonus
        long daysOld = getDaysSinceSubmission();
        if (daysOld > 30)
            score += 5;
        if (daysOld > 60)
            score += 5;

        // Complete application bonus
        if (hasAllRequiredDocuments()) {
            score += 5;
        }

        // Payment completion bonus
        if (applicationFeePaid) {
            score += 5;
        }

        this.priorityScore = score;
    }

    @Override
    public String toString() {
        return "DormitoryApplication{" +
                "id=" + id +
                ", applicationNumber='" + applicationNumber + '\'' +
                ", fullName='" + getFullName() + '\'' +
                ", email='" + email + '\'' +
                ", status=" + status +
                ", priorityScore=" + priorityScore +
                ", submittedAt=" + submittedAt +
                ", assignedRoomNumber='" + assignedRoomNumber + '\'' +
                '}';
    }
}