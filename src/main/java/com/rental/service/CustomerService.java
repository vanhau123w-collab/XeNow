package com.rental.service;

import com.rental.entity.Customer;
import com.rental.entity.User;
import com.rental.repository.CustomerRepository;
import com.rental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

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
    
    public Customer findByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email: " + email));
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
}
