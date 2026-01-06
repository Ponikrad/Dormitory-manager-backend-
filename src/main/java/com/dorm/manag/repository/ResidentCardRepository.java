package com.dorm.manag.repository;

import com.dorm.manag.entity.ResidentCard;
import com.dorm.manag.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResidentCardRepository extends JpaRepository<ResidentCard, Long> {

    Optional<ResidentCard> findByUser(User user);

    Optional<ResidentCard> findByQrCode(String qrCode);

    Optional<ResidentCard> findByUserId(Long userId);

    @Query("SELECT rc FROM ResidentCard rc WHERE rc.isActive = true")
    List<ResidentCard> findActiveCards();

    @Query("SELECT rc FROM ResidentCard rc WHERE rc.expirationDate < :currentDate")
    List<ResidentCard> findExpiredCards(@Param("currentDate") LocalDateTime currentDate);

    @Query("SELECT rc FROM ResidentCard rc WHERE rc.expirationDate BETWEEN :startDate AND :endDate")
    List<ResidentCard> findCardsExpiringBetween(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    boolean existsByQrCode(String qrCode);

    boolean existsByUserId(Long userId);

    @Query("SELECT COUNT(rc) FROM ResidentCard rc WHERE rc.isActive = true")
    long countActiveCards();

    void deleteByUserId(Long userId);
}