package com.dorm.manag.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "reservable_resources")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservableResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false)
    private ResourceType resourceType;

    @Column(name = "location")
    private String location; // e.g., "Floor 2, Room 201"

    @Column(name = "floor_number")
    private Integer floorNumber;

    @Column(name = "room_number")
    private String roomNumber;

    @Column(name = "capacity")
    private Integer capacity; // Maximum number of people

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // Availability settings
    @Column(name = "available_from")
    private LocalTime availableFrom = LocalTime.of(6, 0); // 6:00 AM

    @Column(name = "available_to")
    private LocalTime availableTo = LocalTime.of(23, 0); // 11:00 PM

    @Column(name = "available_days")
    private String availableDays = "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY";

    // Reservation settings
    @Column(name = "min_reservation_duration")
    private Integer minReservationDuration; // in minutes

    @Column(name = "max_reservation_duration")
    private Integer maxReservationDuration; // in minutes

    @Column(name = "reservation_interval")
    private Integer reservationInterval = 30; // booking slots in minutes

    @Column(name = "advance_booking_hours")
    private Integer advanceBookingHours = 0; // minimum hours in advance

    @Column(name = "max_advance_days")
    private Integer maxAdvanceDays = 14; // maximum days in advance

    @Column(name = "requires_approval")
    private Boolean requiresApproval = false;

    // Pricing - most resources are free (users pick up keys at reception)
    @Column(name = "cost_per_hour", precision = 8, scale = 2)
    private BigDecimal costPerHour = BigDecimal.ZERO; // FREE by default

    @Column(name = "deposit_required", precision = 8, scale = 2)
    private BigDecimal depositRequired = BigDecimal.ZERO; // No deposit for most resources

    // Key management
    @Column(name = "requires_key")
    private Boolean requiresKey = true; // Most resources require key pickup

    @Column(name = "key_location")
    private String keyLocation = "Reception"; // Where to get the key

    @Column(name = "key_instructions", columnDefinition = "TEXT")
    private String keyInstructions; // Instructions for key pickup/return

    // Usage limits
    @Column(name = "max_reservations_per_user_per_day")
    private Integer maxReservationsPerUserPerDay = 1;

    @Column(name = "max_duration_per_user_per_day")
    private Integer maxDurationPerUserPerDay; // in minutes

    @Column(name = "cooldown_period")
    private Integer cooldownPeriod = 0; // minutes between reservations

    // Equipment and amenities
    @Column(name = "equipment", columnDefinition = "TEXT")
    private String equipment; // JSON or comma-separated list

    @Column(name = "amenities", columnDefinition = "TEXT")
    private String amenities;

    @Column(name = "rules", columnDefinition = "TEXT")
    private String rules;

    // Contact and maintenance
    @Column(name = "contact_info")
    private String contactInfo;

    @Column(name = "maintenance_notes", columnDefinition = "TEXT")
    private String maintenanceNotes;

    @Column(name = "last_maintenance")
    private LocalDateTime lastMaintenance;

    @Column(name = "next_maintenance")
    private LocalDateTime nextMaintenance;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public ReservableResource(String name, ResourceType resourceType, String location) {
        this.name = name;
        this.resourceType = resourceType;
        this.location = location;
        this.isActive = true;

        // Set defaults based on resource type
        this.minReservationDuration = resourceType.getDefaultDurationMinutes() / 2;
        this.maxReservationDuration = resourceType.getMaxDurationMinutes();
        this.costPerHour = BigDecimal.valueOf(resourceType.getCostPerHour());
        this.maxReservationsPerUserPerDay = resourceType.getMaxReservationsPerDay();
        this.advanceBookingHours = resourceType.getMinAdvanceBookingHours();
        this.requiresApproval = resourceType.requiresApproval();
    }

    // Helper methods
    public boolean isAvailableNow() {
        LocalDateTime now = LocalDateTime.now();
        LocalTime currentTime = now.toLocalTime();
        String currentDay = now.getDayOfWeek().name();

        return isActive &&
                currentTime.isAfter(availableFrom) &&
                currentTime.isBefore(availableTo) &&
                availableDays.contains(currentDay);
    }

    public boolean isAvailableAt(LocalDateTime dateTime) {
        LocalTime time = dateTime.toLocalTime();
        String day = dateTime.getDayOfWeek().name();

        return isActive &&
                time.isAfter(availableFrom) &&
                time.isBefore(availableTo) &&
                availableDays.contains(day);
    }

    public boolean requiresPayment() {
        return costPerHour.compareTo(BigDecimal.ZERO) > 0 ||
                depositRequired.compareTo(BigDecimal.ZERO) > 0;
    }

    public BigDecimal calculateCost(int durationMinutes) {
        if (costPerHour.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        double hours = durationMinutes / 60.0;
        return costPerHour.multiply(BigDecimal.valueOf(hours));
    }

    public boolean isMaintenanceNeeded() {
        return nextMaintenance != null && LocalDateTime.now().isAfter(nextMaintenance);
    }

    public String getDisplayLocation() {
        StringBuilder location = new StringBuilder();

        if (floorNumber != null) {
            location.append("Floor ").append(floorNumber);
        }

        if (roomNumber != null) {
            if (location.length() > 0)
                location.append(", ");
            location.append("Room ").append(roomNumber);
        }

        if (this.location != null && !this.location.trim().isEmpty()) {
            if (location.length() > 0)
                location.append(", ");
            location.append(this.location);
        }

        return location.toString();
    }

    public boolean hasCapacityFor(int numberOfPeople) {
        return capacity == null || capacity >= numberOfPeople;
    }

    @Override
    public String toString() {
        return "ReservableResource{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", resourceType=" + resourceType +
                ", location='" + getDisplayLocation() + '\'' +
                ", capacity=" + capacity +
                ", isActive=" + isActive +
                ", costPerHour=" + costPerHour +
                '}';
    }
}