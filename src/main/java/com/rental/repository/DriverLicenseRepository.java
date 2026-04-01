package com.rental.repository;

import com.rental.entity.DriverLicense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DriverLicenseRepository extends JpaRepository<DriverLicense, Integer> {
    List<DriverLicense> findByCustomer_UserId(Integer userId);
}
