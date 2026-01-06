package com.dorm.manag.repository;

import com.dorm.manag.entity.KeyAssignment;
import com.dorm.manag.entity.KeyType;
import com.dorm.manag.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface KeyAssignmentRepository extends JpaRepository<KeyAssignment, Long> {

    List<KeyAssignment> findByUserOrderByIssuedAtDesc(User user);

    List<KeyAssignment> findByStatus(KeyAssignment.AssignmentStatus status);

    @Query("SELECT ka FROM KeyAssignment ka WHERE ka.user = :user AND ka.status = 'ACTIVE'")
    List<KeyAssignment> findActiveAssignmentsByUser(@Param("user") User user);

    @Query("SELECT ka FROM KeyAssignment ka WHERE ka.user = :user AND ka.key.keyType = :keyType AND ka.status = 'ACTIVE'")
    List<KeyAssignment> findActiveAssignmentsByUserAndKeyType(@Param("user") User user,
            @Param("keyType") KeyType keyType);

    @Query("SELECT ka FROM KeyAssignment ka WHERE ka.expectedReturn < :currentDate AND ka.status = 'ACTIVE'")
    List<KeyAssignment> findOverdueAssignments(@Param("currentDate") LocalDateTime currentDate);
}