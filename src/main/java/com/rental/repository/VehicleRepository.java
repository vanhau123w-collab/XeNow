package com.rental.repository;

import com.rental.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Integer> {
    List<Vehicle> findByStatus(Vehicle.Status status);
    List<Vehicle> findByTypeTypeId(Integer typeId);
    List<Vehicle> findByLocationLocationId(Integer locationId);

    @Query("SELECT v FROM Vehicle v WHERE v.status = 'Available' AND (:typeId IS NULL OR v.type.typeId = :typeId)")
    List<Vehicle> findAvailableByType(Integer typeId);
}
