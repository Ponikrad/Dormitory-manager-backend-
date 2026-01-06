package com.dorm.manag.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateReservationRequest {

    @NotNull(message = "Resource ID is required")
    private Long resourceId;

    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @Min(value = 1, message = "Number of people must be at least 1")
    @Max(value = 50, message = "Number of people cannot exceed 50")
    private Integer numberOfPeople = 1;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;

    public boolean isValid() {
        return resourceId != null &&
                startTime != null &&
                endTime != null &&
                startTime.isBefore(endTime) &&
                startTime.isAfter(LocalDateTime.now()) &&
                numberOfPeople != null &&
                numberOfPeople >= 1 &&
                numberOfPeople <= 50;
    }

    public boolean isValidTimeRange() {
        if (startTime == null || endTime == null)
            return false;

        return startTime.isBefore(endTime) &&
                startTime.isAfter(LocalDateTime.now());
    }

    public long getDurationMinutes() {
        if (startTime == null || endTime == null)
            return 0;
        return java.time.Duration.between(startTime, endTime).toMinutes();
    }

    public boolean isValidDuration() {
        long minutes = getDurationMinutes();
        return minutes >= 30 && minutes <= 480;
    }

    public boolean isWithinAdvanceBookingLimit() {
        if (startTime == null)
            return false;

        LocalDateTime maxAdvance = LocalDateTime.now().plusDays(14);
        return startTime.isBefore(maxAdvance);
    }

    public String getValidationError() {
        if (resourceId == null) {
            return "Resource ID is required";
        }
        if (startTime == null) {
            return "Start time is required";
        }
        if (endTime == null) {
            return "End time is required";
        }
        if (!startTime.isAfter(LocalDateTime.now())) {
            return "Start time must be in the future";
        }
        if (!startTime.isBefore(endTime)) {
            return "End time must be after start time";
        }
        if (!isValidDuration()) {
            return "Duration must be between 30 minutes and 8 hours";
        }
        if (!isWithinAdvanceBookingLimit()) {
            return "Cannot book more than 14 days in advance";
        }
        if (numberOfPeople == null || numberOfPeople < 1 || numberOfPeople > 50) {
            return "Number of people must be between 1 and 50";
        }
        if (notes != null && notes.length() > 500) {
            return "Notes cannot exceed 500 characters";
        }
        return null;
    }

    public static CreateReservationRequest forLaundry(Long resourceId, LocalDateTime startTime) {
        CreateReservationRequest request = new CreateReservationRequest();
        request.setResourceId(resourceId);
        request.setStartTime(startTime);
        request.setEndTime(startTime.plusHours(2)); // 2 hour default for laundry
        request.setNumberOfPeople(1);
        request.setNotes("Laundry reservation");
        return request;
    }

    public static CreateReservationRequest forGameRoom(Long resourceId, LocalDateTime startTime, int people) {
        CreateReservationRequest request = new CreateReservationRequest();
        request.setResourceId(resourceId);
        request.setStartTime(startTime);
        request.setEndTime(startTime.plusHours(1)); // 1 hour default for game room
        request.setNumberOfPeople(people);
        return request;
    }

    public static CreateReservationRequest forStudyRoom(Long resourceId, LocalDateTime startTime,
            LocalDateTime endTime) {
        CreateReservationRequest request = new CreateReservationRequest();
        request.setResourceId(resourceId);
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        request.setNumberOfPeople(1);
        request.setNotes("Study session");
        return request;
    }

    @Override
    public String toString() {
        return "CreateReservationRequest{" +
                "resourceId=" + resourceId +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", duration=" + getDurationMinutes() + " minutes" +
                ", numberOfPeople=" + numberOfPeople +
                '}';
    }
}