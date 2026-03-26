package com.rental.repository;
import com.rental.entity.RentalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RentalHistoryRepository extends JpaRepository<RentalHistory, Integer> {
    Optional<RentalHistory> findByBookingBookingId(Integer bookingId);
}
