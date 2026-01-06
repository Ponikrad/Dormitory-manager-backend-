package com.dorm.manag.service;

import com.dorm.manag.dto.ApplicationDto;
import com.dorm.manag.dto.CreateApplicationRequest;
import com.dorm.manag.entity.ApplicationStatus;
import com.dorm.manag.entity.DormitoryApplication;
import com.dorm.manag.entity.Role;
import com.dorm.manag.entity.User;
import com.dorm.manag.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public ApplicationDto submitApplication(CreateApplicationRequest request) {
        validateApplicationRequest(request);

        Optional<DormitoryApplication> existingApp = applicationRepository
                .findByEmailAndStatusNot(request.getEmail(), ApplicationStatus.COMPLETED);

        if (existingApp.isPresent()) {
            throw new RuntimeException("Application already exists for this email");
        }

        DormitoryApplication application = new DormitoryApplication();
        application.setFirstName(request.getFirstName());
        application.setLastName(request.getLastName());
        application.setDateOfBirth(request.getDateOfBirth());
        application.setGender(request.getGender());
        application.setNationality(request.getNationality());
        application.setIdNumber(request.getIdNumber());
        application.setEmail(request.getEmail());
        application.setPhoneNumber(request.getPhoneNumber());
        application.setEmergencyContactName(request.getEmergencyContactName());
        application.setEmergencyContactPhone(request.getEmergencyContactPhone());

        application.setStreetAddress(request.getStreetAddress());
        application.setCity(request.getCity());
        application.setPostalCode(request.getPostalCode());
        application.setCountry(request.getCountry());

        application.setUniversityName(request.getUniversityName());
        application.setStudentId(request.getStudentId());
        application.setFieldOfStudy(request.getFieldOfStudy());
        application.setYearOfStudy(request.getYearOfStudy());
        application.setExpectedGraduation(request.getExpectedGraduation());
        application.setIsExchangeStudent(request.getIsExchangeStudent());
        application.setExchangeProgram(request.getExchangeProgram());

        application.setPreferredRoomType(request.getPreferredRoomType());
        application.setPreferredFloor(request.getPreferredFloor());
        application.setSmokingPreference(request.getSmokingPreference());
        application.setSpecialNeeds(request.getSpecialNeeds());
        application.setRoommateRequests(request.getRoommateRequests());

        application.setMoveInDate(request.getMoveInDate());
        application.setMoveOutDate(request.getMoveOutDate());
        application.setDurationMonths(request.getDurationMonths());

        application.setApplicantNotes(request.getApplicantNotes());

        application.calculatePriorityScore();

        DormitoryApplication savedApplication = applicationRepository.save(application);

        log.info("Application submitted: {} for {}",
                savedApplication.getApplicationNumber(), savedApplication.getEmail());

        return convertToDto(savedApplication);
    }

    @Transactional(readOnly = true)
    public List<ApplicationDto> getAllApplications() {
        List<DormitoryApplication> applications = applicationRepository.findAllByOrderBySubmittedAtDesc();
        return applications.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApplicationDto> getApplicationsByStatus(ApplicationStatus status) {
        List<DormitoryApplication> applications = applicationRepository.findByStatusOrderBySubmittedAtDesc(status);
        return applications.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApplicationDto> getPendingApplications() {
        List<DormitoryApplication> applications = applicationRepository.findPendingApplications();
        return applications.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<ApplicationDto> getApplicationById(Long id) {
        return applicationRepository.findById(id)
                .map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public Optional<ApplicationDto> getApplicationByEmail(String email) {
        return applicationRepository.findByEmail(email)
                .map(this::convertToDto);
    }

    @Transactional
    public ApplicationDto updateApplication(Long id, CreateApplicationRequest request) {
        DormitoryApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        if (!application.canBeModified()) {
            throw new RuntimeException("Application cannot be modified in current status");
        }

        application.setFirstName(request.getFirstName());
        application.setLastName(request.getLastName());
        application.setPhoneNumber(request.getPhoneNumber());
        application.setPreferredRoomType(request.getPreferredRoomType());
        application.setPreferredFloor(request.getPreferredFloor());
        application.setSpecialNeeds(request.getSpecialNeeds());
        application.setApplicantNotes(request.getApplicantNotes());

        application.calculatePriorityScore();

        DormitoryApplication savedApplication = applicationRepository.save(application);

        log.info("Application updated: {}", application.getApplicationNumber());

        return convertToDto(savedApplication);
    }

    @Transactional
    public ApplicationDto approveApplication(Long id, User approver) {
        DormitoryApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        application.approve(approver);
        DormitoryApplication savedApplication = applicationRepository.save(application);

        log.info("Application approved: {} by {}",
                application.getApplicationNumber(), approver.getUsername());

        return convertToDto(savedApplication);
    }

    @Transactional
    public ApplicationDto rejectApplication(Long id, String reason, User reviewer) {
        DormitoryApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        application.reject(reviewer, reason);
        DormitoryApplication savedApplication = applicationRepository.save(application);

        log.info("Application rejected: {} by {}",
                application.getApplicationNumber(), reviewer.getUsername());

        return convertToDto(savedApplication);
    }

    @Transactional
    public ApplicationDto confirmApplication(Long id) {
        DormitoryApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        application.confirm();
        DormitoryApplication savedApplication = applicationRepository.save(application);

        log.info("Application confirmed: {}", application.getApplicationNumber());

        return convertToDto(savedApplication);
    }

    @Transactional
    public ApplicationDto assignRoom(Long id, String roomNumber, User assignedBy) {
        DormitoryApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        application.assignRoom(roomNumber, assignedBy);

        User newUser = createUserFromApplication(application);
        application.setCreatedUser(newUser);

        DormitoryApplication savedApplication = applicationRepository.save(application);

        log.info("Room {} assigned to application {} by {}",
                roomNumber, application.getApplicationNumber(), assignedBy.getUsername());

        return convertToDto(savedApplication);
    }

    @Transactional
    public ApplicationDto completeApplication(Long id) {
        DormitoryApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        application.complete();
        DormitoryApplication savedApplication = applicationRepository.save(application);

        log.info("Application completed: {}", application.getApplicationNumber());

        return convertToDto(savedApplication);
    }

    @Transactional
    public ApplicationDto withdrawApplication(Long id) {
        DormitoryApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        application.withdraw();
        DormitoryApplication savedApplication = applicationRepository.save(application);

        log.info("Application withdrawn: {}", application.getApplicationNumber());

        return convertToDto(savedApplication);
    }

    @Transactional
    public ApplicationDto requestDocuments(Long id, String missingDocuments, User requestedBy) {
        DormitoryApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        application.requestDocuments(missingDocuments);
        DormitoryApplication savedApplication = applicationRepository.save(application);

        log.info("Documents requested for application: {} by {}",
                application.getApplicationNumber(), requestedBy.getUsername());

        return convertToDto(savedApplication);
    }

    @Transactional(readOnly = true)
    public List<ApplicationDto> getOverdueApplications() {
        LocalDateTime now = LocalDateTime.now();
        List<DormitoryApplication> applications = applicationRepository.findOverdueApplications(now);

        return applications.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ApplicationStatsDto getApplicationStatistics() {
        long totalApplications = applicationRepository.count();
        long pendingApplications = applicationRepository.countByStatus(ApplicationStatus.SUBMITTED) +
                applicationRepository.countByStatus(ApplicationStatus.UNDER_REVIEW) +
                applicationRepository.countByStatus(ApplicationStatus.PENDING_DOCUMENTS);
        long approvedApplications = applicationRepository.countByStatus(ApplicationStatus.APPROVED);
        long completedApplications = applicationRepository.countByStatus(ApplicationStatus.COMPLETED);
        long rejectedApplications = applicationRepository.countByStatus(ApplicationStatus.REJECTED);

        ApplicationStatsDto stats = new ApplicationStatsDto();
        stats.setTotalApplications(totalApplications);
        stats.setPendingApplications(pendingApplications);
        stats.setApprovedApplications(approvedApplications);
        stats.setCompletedApplications(completedApplications);
        stats.setRejectedApplications(rejectedApplications);

        return stats;
    }

    private User createUserFromApplication(DormitoryApplication application) {
        String username = application.getEmail().substring(0, application.getEmail().indexOf("@"));

        String tempPassword = "TempPass" + System.currentTimeMillis() % 10000;

        User user = new User();
        user.setUsername(username);
        user.setEmail(application.getEmail());
        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setFirstName(application.getFirstName());
        user.setLastName(application.getLastName());
        user.setPhoneNumber(application.getPhoneNumber());
        user.setRoomNumber(application.getAssignedRoomNumber());
        user.setRole(Role.STUDENT);
        user.setActive(true);

        User savedUser = userService.createUser(user);

        log.info("User account created for application: {} -> {}",
                application.getApplicationNumber(), username);

        return savedUser;
    }

    private void validateApplicationRequest(CreateApplicationRequest request) {
        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }
        if (request.getLastName() == null || request.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (request.getUniversityName() == null || request.getUniversityName().trim().isEmpty()) {
            throw new IllegalArgumentException("University name is required");
        }
        if (request.getMoveInDate() == null) {
            throw new IllegalArgumentException("Move-in date is required");
        }
        if (request.getMoveOutDate() == null) {
            throw new IllegalArgumentException("Move-out date is required");
        }
        if (request.getMoveInDate().isAfter(request.getMoveOutDate())) {
            throw new IllegalArgumentException("Move-in date cannot be after move-out date");
        }
    }

    private ApplicationDto convertToDto(DormitoryApplication application) {
        ApplicationDto dto = new ApplicationDto();
        dto.setId(application.getId());
        dto.setApplicationNumber(application.getApplicationNumber());
        dto.setFirstName(application.getFirstName());
        dto.setLastName(application.getLastName());
        dto.setFullName(application.getFullName());
        dto.setDateOfBirth(application.getDateOfBirth());
        dto.setGender(application.getGender());
        dto.setNationality(application.getNationality());
        dto.setEmail(application.getEmail());
        dto.setPhoneNumber(application.getPhoneNumber());
        dto.setUniversityName(application.getUniversityName());
        dto.setFieldOfStudy(application.getFieldOfStudy());
        dto.setYearOfStudy(application.getYearOfStudy());
        dto.setIsExchangeStudent(application.getIsExchangeStudent());
        dto.setPreferredRoomType(application.getPreferredRoomType());
        dto.setPreferredFloor(application.getPreferredFloor());
        dto.setMoveInDate(application.getMoveInDate());
        dto.setMoveOutDate(application.getMoveOutDate());
        dto.setDurationMonths(application.getDurationMonths());
        dto.setStatus(application.getStatus());
        dto.setPriorityScore(application.getPriorityScore());
        dto.setSubmittedAt(application.getSubmittedAt());
        dto.setReviewedAt(application.getReviewedAt());
        dto.setApprovedAt(application.getApprovedAt());
        dto.setAssignedRoomNumber(application.getAssignedRoomNumber());
        dto.setRoomAssignedAt(application.getRoomAssignedAt());
        dto.setApplicationFeePaid(application.getApplicationFeePaid());
        dto.setDepositPaid(application.getDepositPaid());
        dto.setApplicantNotes(application.getApplicantNotes());
        dto.setAdminNotes(application.getAdminNotes());
        dto.setRejectionReason(application.getRejectionReason());
        dto.setResponseDeadline(application.getResponseDeadline());

        if (application.getReviewedBy() != null) {
            dto.setReviewedByName(application.getReviewedBy().getFirstName() + " " +
                    application.getReviewedBy().getLastName());
        }

        if (application.getApprovedBy() != null) {
            dto.setApprovedByName(application.getApprovedBy().getFirstName() + " " +
                    application.getApprovedBy().getLastName());
        }

        dto.setApproved(application.isApproved());
        dto.setPending(application.isPending());
        dto.setFinal(application.isFinal());
        dto.setCanBeModified(application.canBeModified());
        dto.setOverdue(application.isOverdue());
        dto.setAge(application.getAge());
        dto.setDaysSinceSubmission(application.getDaysSinceSubmission());
        dto.setDaysUntilDeadline(application.getDaysUntilDeadline());
        dto.setPaymentComplete(application.isPaymentComplete());
        dto.setTotalAmountDue(application.getTotalAmountDue());

        return dto;
    }

    @lombok.Data
    public static class ApplicationStatsDto {
        private long totalApplications;
        private long pendingApplications;
        private long approvedApplications;
        private long completedApplications;
        private long rejectedApplications;
    }
}