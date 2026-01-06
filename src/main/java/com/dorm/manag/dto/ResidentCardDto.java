package com.dorm.manag.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResidentCardDto {

    private Long id;

    private Long userId;

    private String userName;

    private String userEmail;

    private String roomNumber;

    private String qrCode;

    private String cardNumber;

    private String accessLevel;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime issuedDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expirationDate;

    private boolean isActive;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastUsed;

    private Long usageCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    private String status;

    private Long daysUntilExpiration;

    private boolean expired;

    private boolean expiringSoon;

    public static ResidentCardDto createBasic(Long userId, String userName, String qrCode) {
        ResidentCardDto dto = new ResidentCardDto();
        dto.setUserId(userId);
        dto.setUserName(userName);
        dto.setQrCode(qrCode);
        dto.setIssuedDate(LocalDateTime.now());
        dto.setExpirationDate(LocalDateTime.now().plusYears(1));
        dto.setActive(true);
        dto.setUsageCount(0L);
        return dto;
    }

    public void calculateStatus() {
        LocalDateTime now = LocalDateTime.now();

        if (!isActive) {
            this.status = "INACTIVE";
            this.expired = false;
            this.expiringSoon = false;
            this.daysUntilExpiration = 0L;
        } else if (expirationDate != null && now.isAfter(expirationDate)) {
            this.status = "EXPIRED";
            this.expired = true;
            this.expiringSoon = false;
            this.daysUntilExpiration = 0L;
        } else if (expirationDate != null) {
            this.daysUntilExpiration = java.time.temporal.ChronoUnit.DAYS.between(now, expirationDate);
            this.expired = false;
            this.expiringSoon = daysUntilExpiration <= 30 && daysUntilExpiration > 0;
            this.status = expiringSoon ? "EXPIRING_SOON" : "ACTIVE";
        } else {
            this.status = "ACTIVE";
            this.expired = false;
            this.expiringSoon = false;
            this.daysUntilExpiration = 0L;
        }
    }

    public boolean canAccess() {
        return isActive && !isExpired();
    }

    public boolean isExpired() {
        return expired || (expirationDate != null && LocalDateTime.now().isAfter(expirationDate));
    }

    public String getDisplayStatus() {
        calculateStatus();
        return switch (status) {
            case "ACTIVE" -> "Active";
            case "INACTIVE" -> "Inactive";
            case "EXPIRED" -> "Expired";
            case "EXPIRING_SOON" -> "Expiring Soon";
            default -> "Unknown";
        };
    }

    public String getFormattedExpirationInfo() {
        if (isExpired()) {
            return "Expired";
        } else if (daysUntilExpiration != null) {
            if (daysUntilExpiration == 0) {
                return "Expires today";
            } else if (daysUntilExpiration == 1) {
                return "Expires tomorrow";
            } else {
                return "Expires in " + daysUntilExpiration + " days";
            }
        }
        return "No expiration info";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ResidentCardDto dto = new ResidentCardDto();

        public Builder id(Long id) {
            dto.setId(id);
            return this;
        }

        public Builder userId(Long userId) {
            dto.setUserId(userId);
            return this;
        }

        public Builder userName(String userName) {
            dto.setUserName(userName);
            return this;
        }

        public Builder roomNumber(String roomNumber) {
            dto.setRoomNumber(roomNumber);
            return this;
        }

        public Builder qrCode(String qrCode) {
            dto.setQrCode(qrCode);
            return this;
        }

        public Builder issuedDate(LocalDateTime issuedDate) {
            dto.setIssuedDate(issuedDate);
            return this;
        }

        public Builder expirationDate(LocalDateTime expirationDate) {
            dto.setExpirationDate(expirationDate);
            return this;
        }

        public Builder active(boolean active) {
            dto.setActive(active);
            return this;
        }

        public ResidentCardDto build() {
            dto.calculateStatus();
            return dto;
        }
    }
}