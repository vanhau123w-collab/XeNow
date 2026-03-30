package com.rental.service;

import com.rental.entity.Vehicle;
import com.rental.entity.Brand;
import com.rental.entity.Model;
import com.rental.entity.Location;
import java.util.List;

public interface VehicleService {
    List<Vehicle> getAllVehicles();
    List<Vehicle> getAvailableVehicles();
    List<Vehicle> getAvailableByType(String type);
    Vehicle getById(Integer id);
    List<Brand> getAllBrands();
    Brand getBrandById(Integer id);
    List<Model> getAllModels();
    Model getModelById(Integer id);
    List<Model> getModelsByBrand(Integer brandId);
    Vehicle save(Vehicle vehicle);
    void deleteById(Integer id);
    Location getLocationById(Integer id);
    Vehicle updateStatus(Integer vehicleId, Vehicle.Status status);
    Vehicle markAsMaintained(Integer vehicleId);
}
