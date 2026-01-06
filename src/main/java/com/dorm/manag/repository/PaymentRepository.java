package com.dorm.manag.repository;

import com.dorm.manag.entity.Payment;
import com.dorm.manag.entity.PaymentMethod;
import com.dorm.manag.entity.PaymentStatus;
import com.dorm.manag.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Find payments by user
    List<Payment> findByUserOrderByCreatedAtDesc(User user);

    Page<Payment> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    List<Payment> findByUserId(Long userId);

    // Find by status
    List<Payment> findByStatus(PaymentStatus status);

    List<Payment> findByStatusOrderByCreatedAtDesc(PaymentStatus status);

    // Find by payment method
    List<Payment> findByPaymentMethod(PaymentMethod paymentMethod);

    // Find by payment type
    List<Payment> findByPaymentType(String paymentType);

    // Find by date ranges
    @Query("SELECT p FROM Payment p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    List<Payment> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT p FROM Payment p WHERE p.dueDate BETWEEN :startDate AND :endDate")
    List<Payment> findByDueDateBetween(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Find overdue payments
    @Query("SELECT p FROM Payment p WHERE p.dueDate < :currentDate AND p.status != 'COMPLETED'")
    List<Payment> findOverduePayments(@Param("currentDate") LocalDateTime currentDate);

    // Find payments due soon
    @Query("SELECT p FROM Payment p WHERE p.dueDate BETWEEN :startDate AND :endDate AND p.status = 'PENDING'")
    List<Payment> findPaymentsDueSoon(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // User-specific queries
    @Query("SELECT p FROM Payment p WHERE p.user.id = :userId AND p.status = :status")
    List<Payment> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.user.id = :userId AND p.paymentType = :paymentType")
    List<Payment> findByUserIdAndPaymentType(@Param("userId") Long userId, @Param("paymentType") String paymentType);

    // Statistics queries
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = 'COMPLETED'")
    long countCompletedPayments();

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = 'PENDING'")
    long countPendingPayments();

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = 'FAILED'")
    long countFailedPayments();

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'COMPLETED' AND p.createdAt BETWEEN :startDate AND :endDate")
    Optional<BigDecimal> getTotalCompletedAmountBetween(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'COMPLETED' AND p.user.id = :userId")
    Optional<BigDecimal> getTotalPaidByUser(@Param("userId") Long userId);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'PENDING' AND p.user.id = :userId")
    Optional<BigDecimal> getTotalPendingByUser(@Param("userId") Long userId);

    // Monthly statistics
    @Query("SELECT YEAR(p.createdAt), MONTH(p.createdAt), COUNT(p), SUM(p.amount) " +
            "FROM Payment p WHERE p.status = 'COMPLETED' " +
            "GROUP BY YEAR(p.createdAt), MONTH(p.createdAt) " +
            "ORDER BY YEAR(p.createdAt) DESC, MONTH(p.createdAt) DESC")
    List<Object[]> getMonthlyPaymentStatistics();

    // Payment method statistics
    @Query("SELECT p.paymentMethod, COUNT(p), SUM(p.amount) FROM Payment p " +
            "WHERE p.status = 'COMPLETED' AND p.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY p.paymentMethod")
    List<Object[]> getPaymentMethodStatistics(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Room-specific queries
    @Query("SELECT p FROM Payment p WHERE p.roomNumber = :roomNumber ORDER BY p.createdAt DESC")
    List<Payment> findByRoomNumber(@Param("roomNumber") String roomNumber);

    // External payment ID lookup
    Optional<Payment> findByExternalPaymentId(String externalPaymentId);

    Optional<Payment> findByTransactionId(String transactionId);

    // Recent payments
    @Query("SELECT p FROM Payment p ORDER BY p.createdAt DESC")
    Page<Payment> findAllOrderByCreatedAtDesc(Pageable pageable);

    // User's recent payments
    @Query("SELECT p FROM Payment p WHERE p.user.id = :userId AND p.createdAt >= :since")
    List<Payment> findRecentPaymentsByUser(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    // Delete old failed/cancelled payments (for cleanup)
    @Query("DELETE FROM Payment p WHERE p.status IN ('FAILED', 'CANCELLED') AND p.createdAt < :cutoffDate")
    void deleteOldFailedPayments(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Find payment with user eagerly loaded (for PDF generation)
    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.user WHERE p.id = :id")
    Optional<Payment> findByIdWithUser(@Param("id") Long id);
}