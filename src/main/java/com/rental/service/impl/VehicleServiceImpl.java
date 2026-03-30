package com.rental.service.impl;

import com.rental.entity.Vehicle;
import com.rental.entity.Brand;
import com.rental.entity.Model;
import com.rental.entity.Location;
import com.rental.repository.VehicleRepository;
import com.rental.repository.BrandRepository;
import com.rental.repository.ModelRepository;
import com.rental.repository.LocationRepository;
import com.rental.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final LocationRepository locationRepository;
    private final BrandRepository brandRepository;
    private final ModelRepository modelRepository;

    @Override
    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }

    @Override
    public List<Vehicle> getAvailableVehicles() {
        return vehicleRepository.findByStatus(Vehicle.Status.Available);
    }

    @Override
    public List<Vehicle> getAvailableByType(String type) {
        return vehicleRepository.findAvailableByType(type);
    }

    @Override
    public Vehicle getById(Integer id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy xe với ID: " + id));
    }

    @Override
    public List<Brand> getAllBrands() {
        return brandRepository.findAll();
    }

    @Override
    public Brand getBrandById(Integer id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hãng xe với ID: " + id));
    }

    @Override
    public List<Model> getAllModels() {
        return modelRepository.findAll();
    }

    @Override
    public Model getModelById(Integer id) {
        return modelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mẫu xe với ID: " + id));
    }

    @Override
    public List<Model> getModelsByBrand(Integer brandId) {
        return modelRepository.findByBrandBrandId(brandId);
    }

    @Override
    @Transactional
    public Vehicle save(Vehicle vehicle) {
        return vehicleRepository.save(vehicle);
    }

    @Override
    @Transactional
    public void deleteById(Integer id) {
        vehicleRepository.deleteById(id);
    }

    @Override
    public Location getLocationById(Integer id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh với ID: " + id));
    }

    @Override
    @Transactional
    public Vehicle updateStatus(Integer vehicleId, Vehicle.Status status) {
        Vehicle vehicle = getById(vehicleId);
        vehicle.setStatus(status);
        return vehicleRepository.save(vehicle);
    }

    @Override
    @Transactional
    public Vehicle markAsMaintained(Integer vehicleId) {
        Vehicle vehicle = getById(vehicleId);
        vehicle.setLastMaintenanceMileage(vehicle.getMileage());
        vehicle.setStatus(Vehicle.Status.Available);
        return vehicleRepository.save(vehicle);
    }
}
