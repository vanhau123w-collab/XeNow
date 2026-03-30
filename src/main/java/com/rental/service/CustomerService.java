package com.rental.service;

import com.rental.entity.Customer;
import java.util.List;

public interface CustomerService {
    List<Customer> getAll();
    Customer getById(Integer id);
    Customer findByUserId(Integer userId);
    Customer findByEmail(String email);
    Customer register(Customer customer);
}
