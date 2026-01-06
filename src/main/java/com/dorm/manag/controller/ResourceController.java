package com.dorm.manag.controller;

import com.dorm.manag.entity.ReservableResource;
import com.dorm.manag.entity.Reservation;
import com.dorm.manag.entity.ResourceType;
import com.dorm.manag.repository.ReservableResourceRepository;
import com.dorm.manag.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class ResourceController {

    private final ReservableResourceRepository resourceRepository;
    private final ReservationRepository reservationRepository;

    @GetMapping
    public ResponseEntity<?> getAllResources() {
        try {
            List<ReservableResource> resources = resourceRepository.findByIsActiveOrderByResourceTypeAsc(true);
            return ResponseEntity.ok(resources);
        } catch (Exception e) {
            log.error("Error retrieving resources: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve resources"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getResourceById(@PathVariable Long id) {
        try {
            ReservableResource resource = resourceRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Resource not found"));
            return ResponseEntity.ok(resource);
        } catch (Exception e) {
            log.error("Error retrieving resource {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Resource not found"));
        }
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<?> getResourcesByType(@PathVariable String type) {
        try {
            ResourceType resourceType = ResourceType.valueOf(type.toUpperCase());
            List<ReservableResource> resources = resourceRepository.findByResourceTypeAndIsActive(resourceType, true);
            return ResponseEntity.ok(resources);
        } catch (Exception e) {
            log.error("Error retrieving resources by type: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid resource type"));
        }
    }

    @GetMapping("/floor/{floor}")
    public ResponseEntity<?> getResourcesByFloor(@PathVariable Integer floor) {
        try {
            List<ReservableResource> resources = resourceRepository.findByFloorNumberAndIsActive(floor, true);
            return ResponseEntity.ok(resources);
        } catch (Exception e) {
            log.error("Error retrieving resources by floor: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve resources"));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchResources(@RequestParam String query) {
        try {
            List<ReservableResource> resources = resourceRepository.findByNameContaining(query);
            return ResponseEntity.ok(resources);
        } catch (Exception e) {
            log.error("Error searching resources: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to search resources"));
        }
    }

    @GetMapping("/free")
    public ResponseEntity<?> getFreeResources() {
        try {
            List<ReservableResource> resources = resourceRepository.findFreeResources();
            return ResponseEntity.ok(resources);
        } catch (Exception e) {
            log.error("Error retrieving free resources: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve resources"));
        }
    }

    @GetMapping("/available")
    public ResponseEntity<?> getAvailableResources(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        try {
            // Get all active resources
            List<ReservableResource> allResources = resourceRepository.findByIsActiveOrderByResourceTypeAsc(true);
            
            // Filter out resources that have conflicting reservations
            List<ReservableResource> availableResources = allResources.stream()
                    .filter(resource -> {
                        List<Reservation> conflicts = reservationRepository.findConflictingReservations(
                                resource.getId(), startTime, endTime);
                        return conflicts.isEmpty();
                    })
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(availableResources);
        } catch (Exception e) {
            log.error("Error retrieving available resources: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve available resources"));
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createResource(@RequestBody Map<String, Object> request) {
        try {
            ReservableResource resource = new ReservableResource();
            resource.setName(request.get("name").toString());
            resource.setDescription(request.getOrDefault("description", "").toString());
            resource.setResourceType(ResourceType.valueOf(request.get("resourceType").toString().toUpperCase()));
            resource.setLocation(request.getOrDefault("location", "").toString());

            if (request.containsKey("floorNumber")) {
                resource.setFloorNumber(Integer.parseInt(request.get("floorNumber").toString()));
            }
            if (request.containsKey("roomNumber")) {
                resource.setRoomNumber(request.get("roomNumber").toString());
            }
            if (request.containsKey("capacity")) {
                resource.setCapacity(Integer.parseInt(request.get("capacity").toString()));
            }

            resource.setIsActive(true);

            // Defaulty z ResourceType
            ResourceType type = resource.getResourceType();
            resource.setMinReservationDuration(type.getDefaultDurationMinutes() / 2);
            resource.setMaxReservationDuration(type.getMaxDurationMinutes());
            resource.setMaxReservationsPerUserPerDay(type.getMaxReservationsPerDay());
            resource.setRequiresApproval(type.requiresApproval());
            resource.setRequiresKey(true);
            resource.setKeyLocation("Reception");

            ReservableResource savedResource = resourceRepository.save(resource);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Resource created successfully");
            response.put("resource", savedResource);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating resource: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to create resource", "message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateResource(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            ReservableResource resource = resourceRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Resource not found"));

            if (request.containsKey("name")) {
                resource.setName(request.get("name").toString());
            }
            if (request.containsKey("description")) {
                resource.setDescription(request.get("description").toString());
            }
            if (request.containsKey("location")) {
                resource.setLocation(request.get("location").toString());
            }
            if (request.containsKey("capacity")) {
                resource.setCapacity(Integer.parseInt(request.get("capacity").toString()));
            }
            if (request.containsKey("isActive")) {
                resource.setIsActive(Boolean.parseBoolean(request.get("isActive").toString()));
            }

            ReservableResource savedResource = resourceRepository.save(resource);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Resource updated successfully");
            response.put("resource", savedResource);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating resource: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to update resource", "message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deactivateResource(@PathVariable Long id) {
        try {
            ReservableResource resource = resourceRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Resource not found"));

            resource.setIsActive(false);
            resourceRepository.save(resource);

            return ResponseEntity.ok(Map.of("message", "Resource deactivated successfully"));
        } catch (Exception e) {
            log.error("Error deactivating resource: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to deactivate resource"));
        }
    }

    @GetMapping("/types")
    public ResponseEntity<?> getResourceTypes() {
        try {
            List<Map<String, Object>> types = List.of(ResourceType.values())
                    .stream()
                    .map(type -> {
                        Map<String, Object> typeInfo = new HashMap<>();
                        typeInfo.put("name", type.name());
                        typeInfo.put("displayName", type.getDisplayName());
                        typeInfo.put("requiresApproval", type.requiresApproval());
                        typeInfo.put("defaultDuration", type.getDefaultDurationMinutes());
                        typeInfo.put("maxDuration", type.getMaxDurationMinutes());
                        return typeInfo;
                    })
                    .toList();

            return ResponseEntity.ok(types);
        } catch (Exception e) {
            log.error("Error retrieving resource types: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve resource types"));
        }
    }
}