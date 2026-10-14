package com.springboot.stockpilot.auth;

import com.springboot.stockpilot.customer.Customer;
import com.springboot.stockpilot.customer.CustomerRepository;
import com.springboot.stockpilot.user.Role;
import com.springboot.stockpilot.user.RoleRepository;
import com.springboot.stockpilot.user.User;
import com.springboot.stockpilot.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
                       CustomerRepository customerRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request){
        String name= request.getName().trim();
        String email= request.getEmail().trim().toLowerCase(Locale.ROOT);

        if(userRepository.existsByEmail(email) || customerRepository.existsByEmail(email)){
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An account with this email is already exists");
        }

        Role customerRole = roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new IllegalStateException(
                        "CUSTOMER role is missing from the database")
                );

        User user=new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(customerRole);
        user.setActive(true);

        User savedUser=userRepository.saveAndFlush(user);

        Customer customer=new Customer();
        customer.setUser(savedUser);
        customer.setName(name);
        customer.setEmail(email);
        customer.setPhone(normalizeOptional(request.getPhone()));
        customer.setAddress(normalizeOptional(request.getAddress()));
        customer.setActive(true);

        Customer savedCustomer=customerRepository.save(customer);

        return new RegisterResponse(savedUser.getId(), savedCustomer.getId(),
                savedUser.getName(), savedUser.getEmail(),
                savedUser.getRole().getName(), "Customer registered successfully");
    }

    private String normalizeOptional(String address) {
        if(address==null || address.isBlank()){
            return null;
        }
        return address.trim();
    }

}
