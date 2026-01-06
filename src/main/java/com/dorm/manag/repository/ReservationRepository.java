package com.dorm.manag.repository;

import com.dorm.manag.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

        // Find by user
        List<Reservation> findByUserOrderByStartTimeDesc(User user);

        List<Reservation> findByUserId(Long userId);

        // Find by resource
        List<Reservation> findByResourceOrderByStartTimeAsc(ReservableResource resource);

        List<Reservation> findByResourceId(Long resourceId);

        // Find by status
        List<Reservation> findByStatus(ReservationStatus status);

        List<Reservation> findByStatusOrderByStartTimeAsc(ReservationStatus status);

        // Find by date ranges
        @Query("SELECT r FROM Reservation r WHERE r.startTime BETWEEN :start AND :end")
        List<Reservation> findByStartTimeBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

        @Query("SELECT r FROM Reservation r WHERE r.resource.id = :resourceId AND " +
                        "((r.startTime BETWEEN :start AND :end) OR (r.endTime BETWEEN :start AND :end) OR " +
                        "(r.startTime <= :start AND r.endTime >= :end))")
        List<Reservation> findOverlappingReservations(@Param("resourceId") Long resourceId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        // Availability check
        @Query("SELECT r FROM Reservation r WHERE r.resource.id = :resourceId AND " +
                        "r.status IN ('CONFIRMED', 'CHECKED_IN') AND " +
                        "((r.startTime < :endTime AND r.endTime > :startTime))")
        List<Reservation> findConflictingReservations(@Param("resourceId") Long resourceId,
                        @Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime);

        // User limits check
        @Query("SELECT COUNT(r) FROM Reservation r WHERE r.user.id = :userId AND " +
                        "r.resource.id = :resourceId AND " +
                        "DATE(r.startTime) = DATE(:date) AND " +
                        "r.status NOT IN ('CANCELLED', 'NO_SHOW')")
        long countUserReservationsForResourceOnDate(@Param("userId") Long userId,
                        @Param("resourceId") Long resourceId,
                        @Param("date") LocalDateTime date);

        // Find upcoming reservations
        @Query("SELECT r FROM Reservation r WHERE r.status = 'CONFIRMED' AND " +
                        "r.startTime BETWEEN :now AND :futureTime ORDER BY r.startTime ASC")
        List<Reservation> findUpcomingReservations(@Param("now") LocalDateTime now,
                        @Param("futureTime") LocalDateTime futureTime);

        // Find overdue reservations
        @Query("SELECT r FROM Reservation r WHERE r.status IN ('CONFIRMED', 'CHECKED_IN') AND " +
                        "r.endTime < :currentTime")
        List<Reservation> findOverdueReservations(@Param("currentTime") LocalDateTime currentTime);

        // Key management
        @Query("SELECT r FROM Reservation r WHERE r.keyPickedUp = true AND r.keyReturned = false")
        List<Reservation> findReservationsWithUnreturnedKeys();

        @Query("SELECT r FROM Reservation r WHERE r.status = 'CONFIRMED' AND " +
                        "r.keyPickedUp = false AND r.resource.requiresKey = true AND " +
                        "r.startTime BETWEEN :start AND :end")
        List<Reservation> findReadyForKeyPickup(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

        // Statistics
        @Query("SELECT COUNT(r) FROM Reservation r WHERE r.status = 'COMPLETED'")
        long countCompletedReservations();

        @Query("SELECT COUNT(r) FROM Reservation r WHERE r.status = 'NO_SHOW'")
        long countNoShows();

        @Query("SELECT r.resource.resourceType, COUNT(r) FROM Reservation r " +
                        "WHERE r.status = 'COMPLETED' GROUP BY r.resource.resourceType")
        List<Object[]> getReservationStatsByResourceType();

        // Recent activity
        @Query("SELECT r FROM Reservation r WHERE r.createdAt >= :since ORDER BY r.createdAt DESC")
        List<Reservation> findRecentReservations(@Param("since") LocalDateTime since);
}