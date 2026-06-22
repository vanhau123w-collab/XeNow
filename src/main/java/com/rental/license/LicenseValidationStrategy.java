package com.rental.license;

import com.rental.entity.DriverLicense;
import com.rental.entity.Vehicle;

public interface LicenseValidationStrategy {
    boolean supports(Vehicle vehicle);

    boolean isValid(DriverLicense license, Vehicle vehicle);
}
