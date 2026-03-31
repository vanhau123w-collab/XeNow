package com.rental.repository;

import com.rental.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer> {
    List<Booking> findByCustomer_UserIdOrderByStartDateDesc(Integer userId);
    Page<Booking> findByCustomer_UserIdOrderByStartDateDesc(Integer userId, Pageable pageable);
    
    List<Booking> findByStatus(Booking.Status status);
    Page<Booking> findByStatus(Booking.Status status, Pageable pageable);

    List<Booking> findByVehicleVehicleId(Integer vehicleId);
    
    List<Booking> findAllByOrderByStartDateDesc();
    Page<Booking> findAllByOrderByStartDateDesc(Pageable pageable);
}

