package com.rental.repository;

import com.rental.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    
    @Override
    @EntityGraph(attributePaths = {"user"})
    Page<Customer> findAll(Pageable pageable);

    Optional<Customer> findByIdentityCard(String identityCard);
    boolean existsByIdentityCard(String identityCard);
    boolean existsByDriverLicense(String driverLicense);
}
