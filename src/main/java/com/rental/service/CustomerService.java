package com.rental.service;

import com.rental.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface CustomerService {

    List<Customer> getAll();
    Page<Customer> getAll(Pageable pageable);

    public List<Customer> getAll() {
        return customerRepository.findAll();
    }

    public Customer getById(Integer id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));
    }

    public Customer findByUserId(Integer userId) {
        return customerRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với ID: " + userId));
    }
    
    public Customer findByEmail(String identifier) {
        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với định danh: " + identifier));
        return customerRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));
    }
    
    public Customer register(Customer customer) {
        // Check if email already exists
        if (userRepository.findByEmail(customer.getUser().getEmail()).isPresent()) {
            throw new RuntimeException("Email đã được sử dụng");
        }
        if (userRepository.findByUsername(customer.getUser().getUsername()).isPresent()) {
            throw new RuntimeException("Username đã được sử dụng");
        }
        
        // Save user first
        User user = customer.getUser();
        user = userRepository.save(user);
        
        // Set user to customer
        customer.setUser(user);
        customer.setUserId(user.getUserId());
        
        return customerRepository.save(customer);
    }
    Customer getById(Integer id);
    Customer findByUserId(Integer userId);
    Customer findByEmail(String email);
    Customer register(Customer customer);
}
