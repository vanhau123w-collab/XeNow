package com.rental.repository;

import com.rental.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer> {
    List<Booking> findByCustomerCustomerId(Integer customerId);
    List<Booking> findByStatus(Booking.Status status);
    List<Booking> findByVehicleVehicleId(Integer vehicleId);
    List<Booking> findAllByOrderByCreatedAtDesc();
}
