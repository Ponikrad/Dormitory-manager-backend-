package com.dorm.manag.repository;

import com.dorm.manag.entity.ApplicationStatus;
import com.dorm.manag.entity.DormitoryApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<DormitoryApplication, Long> {

    Optional<DormitoryApplication> findByEmail(String email);

    Optional<DormitoryApplication> findByEmailAndStatusNot(String email, ApplicationStatus status);

    List<DormitoryApplication> findAllByOrderBySubmittedAtDesc();

    List<DormitoryApplication> findByStatusOrderBySubmittedAtDesc(ApplicationStatus status);

    @Query("SELECT a FROM DormitoryApplication a WHERE a.status IN ('SUBMITTED', 'UNDER_REVIEW', 'PENDING_DOCUMENTS') ORDER BY a.priorityScore DESC, a.submittedAt ASC")
    List<DormitoryApplication> findPendingApplications();

    @Query("SELECT a FROM DormitoryApplication a WHERE a.responseDeadline < :currentDate AND a.status IN ('PENDING_DOCUMENTS', 'APPROVED')")
    List<DormitoryApplication> findOverdueApplications(@Param("currentDate") LocalDateTime currentDate);

    long countByStatus(ApplicationStatus status);
}