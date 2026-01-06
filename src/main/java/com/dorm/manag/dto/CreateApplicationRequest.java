package com.dorm.manag.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateApplicationRequest {
    // Personal Information
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String gender;
    private String nationality;
    private String idNumber;

    // Contact Information
    private String email;
    private String phoneNumber;
    private String emergencyContactName;
    private String emergencyContactPhone;

    // Address
    private String streetAddress;
    private String city;
    private String postalCode;
    private String country;

    // Academic Information
    private String universityName;
    private String studentId;
    private String fieldOfStudy;
    private Integer yearOfStudy;
    private LocalDate expectedGraduation;
    private Boolean isExchangeStudent;
    private String exchangeProgram;

    // Accommodation Preferences
    private String preferredRoomType;
    private Integer preferredFloor;
    private String smokingPreference;
    private String specialNeeds;
    private String roommateRequests;

    // Stay Period
    private LocalDate moveInDate;
    private LocalDate moveOutDate;
    private Integer durationMonths;

    // Notes
    private String applicantNotes;
}
