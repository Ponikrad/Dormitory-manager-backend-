package com.dorm.manag.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "dormitory_keys")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DormitoryKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "key_code", unique = true, nullable = false)
    private String keyCode; // Unique identifier like "R-201-A" or "LAU-01"

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "key_type", nullable = false)
    private KeyType keyType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KeyStatus status = KeyStatus.AVAILABLE;

    // Location information
    @Column(name = "room_number")
    private String roomNumber; // For room keys

    @Column(name = "floor_number")
    private Integer floorNumber;

    @Column(name = "building_section")
    private String buildingSection;

    @Column(name = "location_notes")
    private String locationNotes;

    // Physical properties
    @Column(name = "key_material")
    private String keyMaterial = "Metal"; // Metal, Plastic, Electronic, etc.

    @Column(name = "key_color")
    private String keyColor;

    @Column(name = "has_tag", nullable = false)
    private Boolean hasTag = true;

    @Column(name = "tag_number")
    private String tagNumber;

    // Security
    @Column(name = "security_level")
    private Integer securityLevel; // 1-5

    @Column(name = "requires_deposit", nullable = false)
    private Boolean requiresDeposit = false;

    @Column(name = "deposit_amount", precision = 8, scale = 2)
    private BigDecimal depositAmount = BigDecimal.ZERO;

    // Maintenance
    @Column(name = "last_maintenance")
    private LocalDateTime lastMaintenance;

    @Column(name = "next_maintenance")
    private LocalDateTime nextMaintenance;

    @Column(name = "maintenance_notes")
    private String maintenanceNotes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Tracking
    @Column(name = "total_assignments")
    private Long totalAssignments = 0L;

    @Column(name = "lost_count")
    private Integer lostCount = 0;

    @Column(name = "last_known_location")
    private String lastKnownLocation;

    @Column(name = "replacement_cost", precision = 8, scale = 2)
    private BigDecimal replacementCost;

    // Associated resource (for temporary keys)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id")
    private ReservableResource associatedResource;

    // Notes
    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "is_master_key", nullable = false)
    private Boolean isMasterKey = false;

    @Column(name = "opens_doors")
    private String opensDoors; // Comma-separated list of rooms/doors this key opens

    // Constructors
    public DormitoryKey(String keyCode, String description, KeyType keyType) {
        this.keyCode = keyCode;
        this.description = description;
        this.keyType = keyType;
        this.status = KeyStatus.AVAILABLE;
        this.securityLevel = keyType.getSecurityLevel();
        this.requiresDeposit = keyType.requiresDeposit();
        this.depositAmount = BigDecimal.valueOf(keyType.getDepositAmount());
    }

    public DormitoryKey(String keyCode, String description, KeyType keyType, String roomNumber) {
        this(keyCode, description, keyType);
        this.roomNumber = roomNumber;
        if (roomNumber != null && roomNumber.length() > 0) {
            this.floorNumber = Integer.parseInt(roomNumber.substring(0, 1));
        }
    }

    // Helper methods
    public boolean canBeIssued() {
        return status.canBeIssued();
    }

    public boolean isCurrentlyIssued() {
        return status.isWithUser();
    }

    public boolean needsMaintenance() {
        return nextMaintenance != null && LocalDateTime.now().isAfter(nextMaintenance);
    }

    public boolean needsAttention() {
        return status.needsAttention() || needsMaintenance();
    }

    public void issue() {
        if (!canBeIssued()) {
            throw new IllegalStateException("Key cannot be issued in current status: " + status);
        }
        this.status = KeyStatus.ISSUED;
        this.totalAssignments++;
    }

    public void returnKey() {
        if (!isCurrentlyIssued()) {
            throw new IllegalStateException("Key is not currently issued");
        }
        this.status = KeyStatus.AVAILABLE;
    }

    public void reportLost() {
        this.status = KeyStatus.LOST;
        this.lostCount++;
    }

    public void reportDamaged(String notes) {
        this.status = KeyStatus.DAMAGED;
        this.maintenanceNotes = notes;
    }

    public void putOutOfService(String reason) {
        this.status = KeyStatus.OUT_OF_SERVICE;
        this.adminNotes = reason;
    }

    public void retire(String reason) {
        this.status = KeyStatus.RETIRED;
        this.adminNotes = reason;
    }

    public void reserve() {
        if (status != KeyStatus.AVAILABLE) {
            throw new IllegalStateException("Can only reserve available keys");
        }
        this.status = KeyStatus.RESERVED;
    }

    public void makeAvailable() {
        if (status == KeyStatus.RESERVED || status == KeyStatus.OUT_OF_SERVICE) {
            this.status = KeyStatus.AVAILABLE;
        }
    }

    public String getFullLocation() {
        StringBuilder location = new StringBuilder();

        if (buildingSection != null) {
            location.append("Building ").append(buildingSection).append(", ");
        }

        if (floorNumber != null) {
            location.append("Floor ").append(floorNumber);
        }

        if (roomNumber != null) {
            if (location.length() > 0)
                location.append(", ");
            location.append("Room ").append(roomNumber);
        }

        if (locationNotes != null) {
            if (location.length() > 0)
                location.append(" - ");
            location.append(locationNotes);
        }

        return location.toString();
    }

    public String getDisplayName() {
        if (roomNumber != null) {
            return keyType.getDisplayName() + " - Room " + roomNumber;
        } else if (associatedResource != null) {
            return keyType.getDisplayName() + " - " + associatedResource.getName();
        } else {
            return keyType.getDisplayName() + " (" + keyCode + ")";
        }
    }

    public boolean isHighSecurityKey() {
        return securityLevel >= 4 || isMasterKey;
    }

    public boolean isTemporaryKey() {
        return keyType.canBeTemporaryIssued();
    }

    public boolean isPermanentKey() {
        return keyType.isPermanentAssignment();
    }

    public long getDaysSinceLastMaintenance() {
        if (lastMaintenance == null)
            return Long.MAX_VALUE;
        return java.time.temporal.ChronoUnit.DAYS.between(lastMaintenance, LocalDateTime.now());
    }

    public boolean isFrequentlyLost() {
        return lostCount >= 3;
    }

    @Override
    public String toString() {
        return "DormitoryKey{" +
                "id=" + id +
                ", keyCode='" + keyCode + '\'' +
                ", description='" + description + '\'' +
                ", keyType=" + keyType +
                ", status=" + status +
                ", roomNumber='" + roomNumber + '\'' +
                ", securityLevel=" + securityLevel +
                ", totalAssignments=" + totalAssignments +
                ", lostCount=" + lostCount +
                '}';
    }
}