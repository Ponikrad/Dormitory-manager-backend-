package com.dorm.manag.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "resident_cards")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResidentCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "qr_code", unique = true, nullable = false)
    private String qrCode;

    @Column(name = "issued_date", nullable = false)
    private LocalDateTime issuedDate;

    @Column(name = "expiration_date", nullable = false)
    private LocalDateTime expirationDate;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "card_number")
    private String cardNumber;

    @Column(name = "access_level")
    private String accessLevel = "BASIC";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_used")
    private LocalDateTime lastUsed;

    @Column(name = "usage_count")
    private Long usageCount = 0L;

    // Constructors
    public ResidentCard(User user, String qrCode) {
        this.user = user;
        this.qrCode = qrCode;
        this.issuedDate = LocalDateTime.now();
        this.expirationDate = LocalDateTime.now().plusYears(1);
        this.isActive = true;
        this.usageCount = 0L;
    }

    public ResidentCard(User user, String qrCode, LocalDateTime expirationDate) {
        this.user = user;
        this.qrCode = qrCode;
        this.issuedDate = LocalDateTime.now();
        this.expirationDate = expirationDate;
        this.isActive = true;
        this.usageCount = 0L;
    }

    // Helper methods
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expirationDate);
    }

    public boolean isValidForAccess() {
        return this.isActive && !isExpired();
    }

    public void recordUsage() {
        this.lastUsed = LocalDateTime.now();
        this.usageCount = (this.usageCount == null ? 0L : this.usageCount) + 1;
    }

    public long getDaysUntilExpiration() {
        if (isExpired())
            return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), this.expirationDate);
    }

    public boolean isExpiringSoon(int daysThreshold) {
        return getDaysUntilExpiration() <= daysThreshold && getDaysUntilExpiration() > 0;
    }

    public String getCardStatus() {
        if (!isActive)
            return "INACTIVE";
        if (isExpired())
            return "EXPIRED";
        if (isExpiringSoon(30))
            return "EXPIRING_SOON";
        return "ACTIVE";
    }

    // Override toString to avoid circular reference with User
    @Override
    public String toString() {
        return "ResidentCard{" +
                "id=" + id +
                ", userId=" + (user != null ? user.getId() : null) +
                ", qrCode='" + qrCode + '\'' +
                ", issuedDate=" + issuedDate +
                ", expirationDate=" + expirationDate +
                ", isActive=" + isActive +
                ", cardNumber='" + cardNumber + '\'' +
                ", accessLevel='" + accessLevel + '\'' +
                ", lastUsed=" + lastUsed +
                ", usageCount=" + usageCount +
                '}';
    }
}