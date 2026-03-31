package com.rental.service;

import com.rental.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface CustomerService {
    List<Customer> getAll();
    Page<Customer> getAll(Pageable pageable);
    Customer getById(Integer id);
    Customer findByUserId(Integer userId);
    Customer findByEmail(String email);
    Customer findByIdentifier(String identifier);
    Customer register(Customer customer);
}
