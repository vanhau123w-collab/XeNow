package com.rental.service;

import com.rental.entity.Vehicle;
import com.rental.repository.VehicleRepository;
import com.rental.repository.VehicleTypeRepository;
import com.rental.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final LocationRepository locationRepository;

    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }

    public List<Vehicle> getAvailableVehicles() {
        return vehicleRepository.findByStatus(Vehicle.Status.Available);
    }

    public List<Vehicle> getAvailableByType(Integer typeId) {
        return vehicleRepository.findAvailableByType(typeId);
    }

    public Vehicle getById(Integer id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy xe với ID: " + id));
    }

    public Vehicle save(Vehicle vehicle) {
        return vehicleRepository.save(vehicle);
    }

    public void deleteById(Integer id) {
        vehicleRepository.deleteById(id);
    }

    public Vehicle updateStatus(Integer vehicleId, Vehicle.Status status) {
        Vehicle vehicle = getById(vehicleId);
        vehicle.setStatus(status);
        return vehicleRepository.save(vehicle);
    }
}
