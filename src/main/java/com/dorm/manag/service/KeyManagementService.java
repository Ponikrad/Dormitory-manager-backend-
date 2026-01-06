package com.dorm.manag.service;

import com.dorm.manag.dto.KeyAssignmentDto;
import com.dorm.manag.dto.KeyDto;
import com.dorm.manag.entity.*;
import com.dorm.manag.repository.KeyAssignmentRepository;
import com.dorm.manag.repository.KeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeyManagementService {

    private final KeyRepository keyRepository;
    private final KeyAssignmentRepository keyAssignmentRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<KeyDto> getAllKeys() {
        List<DormitoryKey> keys = keyRepository.findAll();
        return keys.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<KeyDto> getAvailableKeys() {
        List<DormitoryKey> keys = keyRepository.findByStatus(KeyStatus.AVAILABLE);
        return keys.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<KeyDto> getKeysByType(KeyType keyType) {
        List<DormitoryKey> keys = keyRepository.findByKeyType(keyType);
        return keys.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<KeyDto> getKeyById(Long id) {
        return keyRepository.findById(id)
                .map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public List<KeyDto> getUserKeys(User user) {
        List<KeyAssignment> assignments = keyAssignmentRepository.findActiveAssignmentsByUser(user);
        return assignments.stream()
                .map(assignment -> convertToDto(assignment.getKey()))
                .collect(Collectors.toList());
    }

    @Transactional
    public KeyAssignmentDto issueKey(Long keyId, Long userId, User issuedBy,
            KeyAssignment.AssignmentType assignmentType, String notes) {

        DormitoryKey key = keyRepository.findById(keyId)
                .orElseThrow(() -> new RuntimeException("Key not found"));

        User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!key.canBeIssued()) {
            throw new RuntimeException("Key cannot be issued in current status: " + key.getStatus());
        }

        if (key.getKeyType().isPermanentAssignment()) {
            List<KeyAssignment> existingAssignments = keyAssignmentRepository
                    .findActiveAssignmentsByUserAndKeyType(user, key.getKeyType());
            if (!existingAssignments.isEmpty()) {
                throw new RuntimeException("User already has a key of this type");
            }
        }

        key.issue();
        keyRepository.save(key);

        KeyAssignment assignment = new KeyAssignment(key, user, issuedBy, assignmentType);
        assignment.setIssueNotes(notes);

        KeyAssignment savedAssignment = keyAssignmentRepository.save(assignment);

        log.info("Key {} issued to user {} by {}", key.getKeyCode(), user.getUsername(),
                issuedBy.getUsername());

        return convertToDto(savedAssignment);
    }

    @Transactional
    public KeyAssignmentDto returnKey(Long assignmentId, User returnedTo,
            String condition, String notes) {

        KeyAssignment assignment = keyAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        // Allow returning lost keys (status LOST) as well as active ones
        if (!assignment.isActive() && !assignment.isLost()) {
            throw new RuntimeException("Assignment is not active or lost");
        }

        assignment.returnKey(returnedTo, condition, notes);
        
        // If key was lost, mark it as found and available
        DormitoryKey key = assignment.getKey();
        if (key.getStatus() == KeyStatus.LOST) {
            key.setStatus(KeyStatus.AVAILABLE);
        } else {
            key.returnKey();
        }

        keyRepository.save(key);
        KeyAssignment savedAssignment = keyAssignmentRepository.save(assignment);

        log.info("Key {} returned by user {} to {}",
                key.getKeyCode(),
                assignment.getUser().getUsername(),
                returnedTo.getUsername());

        return convertToDto(savedAssignment);
    }

    @Transactional
    public void reportKeyLost(Long assignmentId, User reportedBy) {
        KeyAssignment assignment = keyAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        if (!assignment.isActive()) {
            throw new RuntimeException("Assignment is not active");
        }

        assignment.reportLost();

        DormitoryKey key = assignment.getKey();
        key.reportLost();

        keyRepository.save(key);
        keyAssignmentRepository.save(assignment);

        log.warn("Key {} reported lost by user {}",
                key.getKeyCode(), assignment.getUser().getUsername());
    }

    @Transactional
    public void reportKeyDamaged(Long keyId, String damageDescription, User reportedBy) {
        DormitoryKey key = keyRepository.findById(keyId)
                .orElseThrow(() -> new RuntimeException("Key not found"));

        key.reportDamaged(damageDescription);
        keyRepository.save(key);

        log.info("Key {} reported damaged by {}: {}",
                key.getKeyCode(), reportedBy.getUsername(), damageDescription);
    }

    @Transactional
    public KeyDto createKey(String keyCode, String description, KeyType keyType,
            String roomNumber, User createdBy) {

        if (keyRepository.existsByKeyCode(keyCode)) {
            throw new RuntimeException("Key code already exists: " + keyCode);
        }

        DormitoryKey key = new DormitoryKey(keyCode, description, keyType, roomNumber);
        DormitoryKey savedKey = keyRepository.save(key);

        log.info("Key created: {} by {}", keyCode, createdBy.getUsername());

        return convertToDto(savedKey);
    }

    @Transactional
    public KeyDto updateKey(Long keyId, String description, KeyType keyType,
            String roomNumber, User updatedBy) {

        DormitoryKey key = keyRepository.findById(keyId)
                .orElseThrow(() -> new RuntimeException("Key not found"));

        key.setDescription(description);
        key.setKeyType(keyType);
        key.setRoomNumber(roomNumber);

        if (roomNumber != null && roomNumber.length() > 0) {
            key.setFloorNumber(Integer.parseInt(roomNumber.substring(0, 1)));
        }

        DormitoryKey savedKey = keyRepository.save(key);

        log.info("Key updated: {} by {}", key.getKeyCode(), updatedBy.getUsername());

        return convertToDto(savedKey);
    }

    @Transactional(readOnly = true)
    public List<KeyAssignmentDto> getActiveAssignments() {
        List<KeyAssignment> assignments = keyAssignmentRepository.findByStatus(
                KeyAssignment.AssignmentStatus.ACTIVE);

        return assignments.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<KeyAssignmentDto> getAllAssignments() {
        // Get both active and lost assignments
        List<KeyAssignment> activeAssignments = keyAssignmentRepository.findByStatus(KeyAssignment.AssignmentStatus.ACTIVE);
        List<KeyAssignment> lostAssignments = keyAssignmentRepository.findByStatus(KeyAssignment.AssignmentStatus.LOST);
        
        List<KeyAssignment> allAssignments = new java.util.ArrayList<>();
        allAssignments.addAll(activeAssignments);
        allAssignments.addAll(lostAssignments);

        return allAssignments.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<KeyAssignmentDto> getOverdueAssignments() {
        LocalDateTime now = LocalDateTime.now();
        List<KeyAssignment> assignments = keyAssignmentRepository.findOverdueAssignments(now);

        return assignments.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<KeyAssignmentDto> getUserAssignments(User user) {
        List<KeyAssignment> assignments = keyAssignmentRepository.findByUserOrderByIssuedAtDesc(user);

        return assignments.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public KeyAssignmentDto extendAssignment(Long assignmentId, LocalDateTime newExpectedReturn,
            String reason, User extendedBy) {

        KeyAssignment assignment = keyAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        assignment.extend(newExpectedReturn, reason);
        KeyAssignment savedAssignment = keyAssignmentRepository.save(assignment);

        log.info("Key assignment {} extended until {} by {}",
                assignmentId, newExpectedReturn, extendedBy.getUsername());

        return convertToDto(savedAssignment);
    }

    @Transactional(readOnly = true)
    public List<KeyDto> getKeysNeedingAttention() {
        List<DormitoryKey> keys = keyRepository.findKeysNeedingAttention();

        return keys.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public KeyStatsDto getKeyStatistics() {
        long totalKeys = keyRepository.count();
        long availableKeys = keyRepository.countByStatus(KeyStatus.AVAILABLE);
        long issuedKeys = keyRepository.countByStatus(KeyStatus.ISSUED);
        long lostKeys = keyRepository.countByStatus(KeyStatus.LOST);
        long damagedKeys = keyRepository.countByStatus(KeyStatus.DAMAGED);

        KeyStatsDto stats = new KeyStatsDto();
        stats.setTotalKeys(totalKeys);
        stats.setAvailableKeys(availableKeys);
        stats.setIssuedKeys(issuedKeys);
        stats.setLostKeys(lostKeys);
        stats.setDamagedKeys(damagedKeys);

        return stats;
    }

    private KeyDto convertToDto(DormitoryKey key) {
        KeyDto dto = new KeyDto();
        dto.setId(key.getId());
        dto.setKeyCode(key.getKeyCode());
        dto.setDescription(key.getDescription());
        dto.setKeyType(key.getKeyType());
        dto.setStatus(key.getStatus());
        dto.setRoomNumber(key.getRoomNumber());
        dto.setFloorNumber(key.getFloorNumber());
        dto.setBuildingSection(key.getBuildingSection());
        dto.setLocationNotes(key.getLocationNotes());
        dto.setSecurityLevel(key.getSecurityLevel());
        dto.setRequiresDeposit(key.getRequiresDeposit());
        dto.setDepositAmount(key.getDepositAmount());
        dto.setTotalAssignments(key.getTotalAssignments());
        dto.setLostCount(key.getLostCount());
        dto.setReplacementCost(key.getReplacementCost());
        dto.setIsMasterKey(key.getIsMasterKey());
        dto.setCreatedAt(key.getCreatedAt());
        dto.setUpdatedAt(key.getUpdatedAt());

        dto.setDisplayName(key.getDisplayName());
        dto.setFullLocation(key.getFullLocation());
        dto.setCanBeIssued(key.canBeIssued());
        dto.setCurrentlyIssued(key.isCurrentlyIssued());
        dto.setNeedsAttention(key.needsAttention());
        dto.setHighSecurityKey(key.isHighSecurityKey());
        dto.setTemporaryKey(key.isTemporaryKey());
        dto.setPermanentKey(key.isPermanentKey());

        return dto;
    }

    private KeyAssignmentDto convertToDto(KeyAssignment assignment) {
        KeyAssignmentDto dto = new KeyAssignmentDto();
        dto.setId(assignment.getId());
        dto.setKeyId(assignment.getKey().getId());
        dto.setKeyCode(assignment.getKey().getKeyCode());
        dto.setKeyDescription(assignment.getKey().getDescription());
        dto.setUserId(assignment.getUser().getId());
        dto.setUserName(assignment.getUser().getFirstName() + " " + assignment.getUser().getLastName());
        dto.setAssignmentType(assignment.getAssignmentType());
        dto.setStatus(assignment.getStatus());
        dto.setIssuedAt(assignment.getIssuedAt());
        dto.setExpectedReturn(assignment.getExpectedReturn());
        dto.setReturnedAt(assignment.getReturnedAt());
        dto.setDepositAmount(assignment.getDepositAmount());
        dto.setDepositPaid(assignment.getDepositPaid());
        dto.setFineAmount(assignment.getFineAmount());
        dto.setReplacementCost(assignment.getReplacementCost());
        dto.setIssueNotes(assignment.getIssueNotes());
        dto.setReturnNotes(assignment.getReturnNotes());
        dto.setConditionOnIssue(assignment.getConditionOnIssue());
        dto.setConditionOnReturn(assignment.getConditionOnReturn());

        if (assignment.getIssuedBy() != null) {
            dto.setIssuedByName(assignment.getIssuedBy().getFirstName() + " " +
                    assignment.getIssuedBy().getLastName());
        }

        if (assignment.getReturnedTo() != null) {
            dto.setReturnedToName(assignment.getReturnedTo().getFirstName() + " " +
                    assignment.getReturnedTo().getLastName());
        }

        dto.setActive(assignment.isActive());
        dto.setOverdueNow(assignment.isOverdueNow());
        dto.setHoursOverdue(assignment.getHoursOverdue());
        dto.setDaysOverdue(assignment.getDaysOverdue());
        dto.setTotalAmountOwed(assignment.getTotalAmountOwed());
        dto.setAssignmentSummary(assignment.getAssignmentSummary());

        return dto;
    }

    @lombok.Data
    public static class KeyStatsDto {
        private long totalKeys;
        private long availableKeys;
        private long issuedKeys;
        private long lostKeys;
        private long damagedKeys;
    }
}