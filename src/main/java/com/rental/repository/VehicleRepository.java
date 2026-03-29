package com.rental.repository;

import com.rental.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Integer> {
    List<Vehicle> findByStatus(Vehicle.Status status);
    List<Vehicle> findByType(String type);
    // Fix: Vehicle field is now currentLocation, not location
    List<Vehicle> findByCurrentLocationLocationId(Integer locationId);
    long countByCurrentLocationLocationId(Integer locationId);

    @Query("SELECT v FROM Vehicle v WHERE v.status = 'Available' AND (:type IS NULL OR v.type = :type)")
    List<Vehicle> findAvailableByType(String type);
}
