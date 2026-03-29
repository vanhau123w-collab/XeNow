package com.rental.service;

import com.rental.entity.Vehicle;
import com.rental.entity.Brand;
import com.rental.entity.Model;
import com.rental.repository.VehicleRepository;
import com.rental.repository.BrandRepository;
import com.rental.repository.ModelRepository;
import com.rental.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final LocationRepository locationRepository;
    private final BrandRepository brandRepository;
    private final ModelRepository modelRepository;

    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }

    public List<Vehicle> getAvailableVehicles() {
        return vehicleRepository.findByStatus(Vehicle.Status.Available);
    }

    public List<Vehicle> getAvailableByType(String type) {
        return vehicleRepository.findAvailableByType(type);
    }

    public Vehicle getById(Integer id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy xe với ID: " + id));
    }

    public List<Brand> getAllBrands() {
        return brandRepository.findAll();
    }

    public Brand getBrandById(Integer id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hãng xe với ID: " + id));
    }

    public List<Model> getAllModels() {
        return modelRepository.findAll();
    }

    public Model getModelById(Integer id) {
        return modelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mẫu xe với ID: " + id));
    }

    public List<Model> getModelsByBrand(Integer brandId) {
        return modelRepository.findByBrandBrandId(brandId);
    }

    public Vehicle save(Vehicle vehicle) {
        return vehicleRepository.save(vehicle);
    }

    public void deleteById(Integer id) {
        vehicleRepository.deleteById(id);
    }

    public com.rental.entity.Location getLocationById(Integer id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh với ID: " + id));
    }

    public Vehicle updateStatus(Integer vehicleId, Vehicle.Status status) {
        Vehicle vehicle = getById(vehicleId);
        vehicle.setStatus(status);
        return vehicleRepository.save(vehicle);
    }

    public Vehicle markAsMaintained(Integer vehicleId) {
        Vehicle vehicle = getById(vehicleId);
        vehicle.setLastMaintenanceMileage(vehicle.getMileage());
        vehicle.setStatus(Vehicle.Status.Available);
        return vehicleRepository.save(vehicle);
    }
}
