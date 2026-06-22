package com.rental.license;

import com.rental.entity.DriverLicense;
import com.rental.entity.Vehicle;
import org.springframework.stereotype.Component;

@Component
public class MotorbikeLicenseValidationStrategy implements LicenseValidationStrategy {

    @Override
    public boolean supports(Vehicle vehicle) {
        String type = vehicle.getType() != null ? vehicle.getType().toLowerCase() : "";
        return type.contains("xe số") || type.contains("tay ga");
    }

    @Override
    public boolean isValid(DriverLicense license, Vehicle vehicle) {
        String licenseClass = LicenseValidationService.normalizedClass(license);
        int capacity = vehicle.getEngineCapacity() != null ? vehicle.getEngineCapacity() : 110;

        if (LicenseValidationService.isOldLawLicense(license)) {
            if (capacity < 175) {
                return licenseClass.equals("A1") || licenseClass.equals("A2");
            }
            return licenseClass.equals("A2");
        }

        if (capacity <= 125) {
            return licenseClass.equals("A1") || licenseClass.equals("A");
        }
        return licenseClass.equals("A");
    }
}
