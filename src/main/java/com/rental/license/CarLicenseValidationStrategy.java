package com.rental.license;

import com.rental.entity.DriverLicense;
import com.rental.entity.Vehicle;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class CarLicenseValidationStrategy implements LicenseValidationStrategy {

    @Override
    public boolean supports(Vehicle vehicle) {
        String type = vehicle.getType() != null ? vehicle.getType().toLowerCase() : "";
        return !type.contains("xe số") && !type.contains("tay ga");
    }

    @Override
    public boolean isValid(DriverLicense license, Vehicle vehicle) {
        String licenseClass = LicenseValidationService.normalizedClass(license);
        int seats = vehicle.getSeats() != null ? vehicle.getSeats() : 4;

        if (seats <= 9) {
            if (LicenseValidationService.isOldLawLicense(license)) {
                return Set.of("B1", "B2", "C", "D", "E").contains(licenseClass);
            }
            return Set.of("B", "C1", "C", "D", "D1", "D2").contains(licenseClass);
        }

        if (seats <= 30) {
            if (LicenseValidationService.isOldLawLicense(license)) {
                return Set.of("D", "E").contains(licenseClass);
            }
            return Set.of("D1", "D2", "D").contains(licenseClass);
        }

        return licenseClass.equals("E");
    }
}
