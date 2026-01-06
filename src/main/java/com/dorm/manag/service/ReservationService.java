package com.dorm.manag.service;

import com.dorm.manag.dto.CreateReservationRequest;
import com.dorm.manag.dto.ReservationDto;
import com.dorm.manag.entity.*;
import com.dorm.manag.repository.ReservableResourceRepository;
import com.dorm.manag.repository.ReservationRepository;
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
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservableResourceRepository resourceRepository;
    private final NotificationService notificationService;

    @Transactional
    public ReservationDto createReservation(CreateReservationRequest request, User user) {
        log.info("Creating reservation for user: {} and resource: {}", user.getUsername(), request.getResourceId());

        ReservableResource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new RuntimeException("Resource not found"));

        validateReservation(request, resource, user);

        List<Reservation> conflicts = reservationRepository.findConflictingReservations(
                request.getResourceId(),
                request.getStartTime(),
                request.getEndTime());

        if (!conflicts.isEmpty()) {
            throw new RuntimeException("Resource is already reserved for this time slot");
        }

        Reservation reservation = new Reservation(user, resource, request.getStartTime(), request.getEndTime());
        reservation.setNumberOfPeople(request.getNumberOfPeople());
        reservation.setNotes(request.getNotes());

        Reservation savedReservation = reservationRepository.save(reservation);

        notificationService.notifyReservationConfirmed(
                user,
                resource.getName(),
                request.getStartTime().toString());

        log.info("Reservation created: {}", savedReservation.getId());

        return convertToDto(savedReservation);
    }

    @Transactional(readOnly = true)
    public List<ReservationDto> getUserReservations(User user) {
        List<Reservation> reservations = reservationRepository.findByUserOrderByStartTimeDesc(user);
        return reservations.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReservationDto> getUpcomingReservations() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime futureLimit = now.plusDays(7);
        List<Reservation> reservations = reservationRepository.findUpcomingReservations(now, futureLimit);

        return reservations.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<ReservationDto> getReservationById(Long id) {
        return reservationRepository.findById(id)
                .map(this::convertToDto);
    }

    @Transactional
    public ReservationDto cancelReservation(Long id, User user) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        if (!reservation.getUser().getId().equals(user.getId()) && !user.getRole().equals(Role.ADMIN)) {
            throw new RuntimeException("You can only cancel your own reservations");
        }

        if (!reservation.canBeCancelled()) {
            throw new RuntimeException("Reservation cannot be cancelled at this stage");
        }

        reservation.cancel("Cancelled by user");
        Reservation savedReservation = reservationRepository.save(reservation);

        log.info("Reservation {} cancelled by {}", id, user.getUsername());

        return convertToDto(savedReservation);
    }

    @Transactional
    public ReservationDto checkInReservation(Long id, User user) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        if (!reservation.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You can only check-in your own reservations");
        }

        if (!reservation.canCheckIn()) {
            throw new RuntimeException("Cannot check-in at this time");
        }

        reservation.checkIn();
        Reservation savedReservation = reservationRepository.save(reservation);

        log.info("User {} checked in to reservation {}", user.getUsername(), id);

        return convertToDto(savedReservation);
    }

    @Transactional
    public ReservationDto completeReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        reservation.complete();
        Reservation savedReservation = reservationRepository.save(reservation);

        log.info("Reservation {} completed", id);

        return convertToDto(savedReservation);
    }

    @Transactional(readOnly = true)
    public List<ReservationDto> getResourceReservations(Long resourceId, LocalDateTime start, LocalDateTime end) {
        List<Reservation> reservations = reservationRepository.findByStartTimeBetween(start, end);

        return reservations.stream()
                .filter(r -> r.getResource().getId().equals(resourceId))
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReservationDto> getAllReservations() {
        List<Reservation> reservations = reservationRepository.findAll();
        return reservations.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private void validateReservation(CreateReservationRequest request, ReservableResource resource, User user) {
        if (!resource.getIsActive()) {
            throw new RuntimeException("Resource is not available");
        }

        if (request.getStartTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Cannot book in the past");
        }

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new RuntimeException("End time must be after start time");
        }

        long durationMinutes = java.time.Duration.between(request.getStartTime(), request.getEndTime()).toMinutes();

        if (resource.getMinReservationDuration() != null && durationMinutes < resource.getMinReservationDuration()) {
            throw new RuntimeException(
                    "Reservation too short. Minimum: " + resource.getMinReservationDuration() + " minutes");
        }

        if (resource.getMaxReservationDuration() != null && durationMinutes > resource.getMaxReservationDuration()) {
            throw new RuntimeException(
                    "Reservation too long. Maximum: " + resource.getMaxReservationDuration() + " minutes");
        }

        long userReservationsToday = reservationRepository.countUserReservationsForResourceOnDate(
                user.getId(),
                resource.getId(),
                request.getStartTime());

        if (userReservationsToday >= resource.getMaxReservationsPerUserPerDay()) {
            throw new RuntimeException("You have reached the daily reservation limit for this resource");
        }

        if (request.getNumberOfPeople() != null && !resource.hasCapacityFor(request.getNumberOfPeople())) {
            throw new RuntimeException("Resource capacity exceeded. Max: " + resource.getCapacity());
        }
    }

    private ReservationDto convertToDto(Reservation reservation) {
        ReservationDto dto = new ReservationDto();

        dto.setId(reservation.getId());

        dto.setUserId(reservation.getUser().getId());
        dto.setUserFullName(reservation.getUser().getFirstName() + " " + reservation.getUser().getLastName());
        dto.setUserEmail(reservation.getUser().getEmail());
        dto.setUserRoomNumber(reservation.getUser().getRoomNumber());

        dto.setResourceId(reservation.getResource().getId());
        dto.setResourceName(reservation.getResource().getName());
        dto.setResourceType(reservation.getResource().getResourceType());
        dto.setResourceLocation(reservation.getResource().getLocation());
        dto.setKeyLocation(reservation.getResource().getKeyLocation());
        dto.setRequiresKey(reservation.getResource().getRequiresKey());

        dto.setStartTime(reservation.getStartTime());
        dto.setEndTime(reservation.getEndTime());

        dto.setStatus(reservation.getStatus());
        dto.setNumberOfPeople(reservation.getNumberOfPeople());
        dto.setNotes(reservation.getNotes());
        dto.setAdminNotes(reservation.getAdminNotes());

        dto.setCreatedAt(reservation.getCreatedAt());
        dto.setConfirmedAt(reservation.getConfirmedAt());
        dto.setCheckedInAt(reservation.getCheckedInAt());
        dto.setCompletedAt(reservation.getCompletedAt());
        dto.setCancelledAt(reservation.getCancelledAt());

        dto.setTotalCost(reservation.getTotalCost());
        dto.setPaymentStatus(reservation.getPaymentStatus());

        dto.setKeyPickedUp(reservation.getKeyPickedUp());
        dto.setKeyPickedUpAt(reservation.getKeyPickedUpAt());
        dto.setKeyPickedUpBy(reservation.getKeyPickedUpBy());
        dto.setKeyReturned(reservation.getKeyReturned());
        dto.setKeyReturnedAt(reservation.getKeyReturnedAt());
        dto.setKeyReturnedTo(reservation.getKeyReturnedTo());

        dto.setCancellationReason(reservation.getCancellationReason());
        dto.setReminderSent(reservation.getReminderSent());

        dto.calculateFields();

        return dto;
    }
}