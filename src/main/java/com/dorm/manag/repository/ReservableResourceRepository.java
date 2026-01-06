package com.dorm.manag.repository;

import com.dorm.manag.entity.ReservableResource;
import com.dorm.manag.entity.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservableResourceRepository extends JpaRepository<ReservableResource, Long> {

    // Find by type
    List<ReservableResource> findByResourceType(ResourceType resourceType);

    List<ReservableResource> findByResourceTypeAndIsActive(ResourceType resourceType, Boolean isActive);

    // Find active resources
    List<ReservableResource> findByIsActiveOrderByResourceTypeAsc(Boolean isActive);

    // Find by location
    List<ReservableResource> findByFloorNumber(Integer floorNumber);

    List<ReservableResource> findByFloorNumberAndIsActive(Integer floorNumber, Boolean isActive);

    @Query("SELECT r FROM ReservableResource r WHERE r.location LIKE %:location% AND r.isActive = true")
    List<ReservableResource> findByLocationContaining(@Param("location") String location);

    // Find by capacity
    @Query("SELECT r FROM ReservableResource r WHERE r.capacity >= :minCapacity AND r.isActive = true")
    List<ReservableResource> findByMinCapacity(@Param("minCapacity") Integer minCapacity);

    // Find free resources
    @Query("SELECT r FROM ReservableResource r WHERE r.costPerHour = 0 AND r.isActive = true")
    List<ReservableResource> findFreeResources();

    // Find resources needing maintenance
    @Query("SELECT r FROM ReservableResource r WHERE r.nextMaintenance < :currentTime AND r.isActive = true")
    List<ReservableResource> findResourcesNeedingMaintenance(@Param("currentTime") LocalDateTime currentTime);

    // Search by name
    @Query("SELECT r FROM ReservableResource r WHERE r.name LIKE %:name% AND r.isActive = true")
    List<ReservableResource> findByNameContaining(@Param("name") String name);
}