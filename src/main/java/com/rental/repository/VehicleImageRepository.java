package com.rental.repository;

import com.rental.entity.Vehicle;
import com.rental.entity.VehicleImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleImageRepository extends JpaRepository<VehicleImage, Integer> {
    List<VehicleImage> findByVehicle(Vehicle vehicle);
}
