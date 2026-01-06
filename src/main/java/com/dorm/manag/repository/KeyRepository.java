package com.dorm.manag.repository;

import com.dorm.manag.entity.DormitoryKey;
import com.dorm.manag.entity.KeyStatus;
import com.dorm.manag.entity.KeyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KeyRepository extends JpaRepository<DormitoryKey, Long> {

    List<DormitoryKey> findByStatus(KeyStatus status);

    List<DormitoryKey> findByKeyType(KeyType keyType);

    boolean existsByKeyCode(String keyCode);

    long countByStatus(KeyStatus status);

    @Query("SELECT k FROM DormitoryKey k WHERE k.status IN ('LOST', 'DAMAGED', 'OUT_OF_SERVICE')")
    List<DormitoryKey> findKeysNeedingAttention();
}