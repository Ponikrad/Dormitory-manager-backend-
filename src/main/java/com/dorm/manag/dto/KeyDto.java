package com.dorm.manag.dto;

import com.dorm.manag.entity.KeyStatus;
import com.dorm.manag.entity.KeyType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class KeyDto {
    private Long id;
    private String keyCode;
    private String description;
    private KeyType keyType;
    private KeyStatus status;
    private String roomNumber;
    private Integer floorNumber;
    private String buildingSection;
    private String locationNotes;
    private Integer securityLevel;
    private Boolean requiresDeposit;
    private BigDecimal depositAmount;
    private Long totalAssignments;
    private Integer lostCount;
    private BigDecimal replacementCost;
    private Boolean isMasterKey;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String displayName;
    private String fullLocation;
    private Boolean canBeIssued;
    private Boolean currentlyIssued;
    private Boolean needsAttention;
    private Boolean highSecurityKey;
    private Boolean temporaryKey;
    private Boolean permanentKey;
}
