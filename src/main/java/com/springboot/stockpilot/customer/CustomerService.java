package com.springboot.stockpilot.customer;

import com.springboot.stockpilot.user.User;
import com.springboot.stockpilot.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    public CustomerService(CustomerRepository customerRepository,
            UserRepository userRepository) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public CustomerProfileResponse getMyProfile(Authentication authentication) {
        Customer customer = getAuthenticatedCustomer(authentication);

        return toResponse(customer);
    }

    @Transactional
    public CustomerProfileResponse updateMyProfile(UpdateCustomerProfileRequest request,
            Authentication authentication) {

        Customer customer = getAuthenticatedCustomer(authentication);
        String name = request.name().trim();

        if (name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Name cannot be blank"
            );
        }
        customer.setName(name);
        customer.setPhone(normalizeOptional(request.phone()));
        customer.setAddress(normalizeOptional(request.address()));

        User user = customer.getUser();
        user.setName(name);

        return toResponse(customer);
    }

    private Customer getAuthenticatedCustomer(Authentication authentication) {

        if (authentication == null || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Authentication required"
            );
        }
        String email = authentication.getName().trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "User not found"
                ));

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "User account is inactive"
            );
        }

        Customer customer = customerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Customer profile not found"
                ));

        if (!customer.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Customer account is inactive"
            );
        }

        return customer;
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private CustomerProfileResponse toResponse(Customer customer) {
        return new CustomerProfileResponse(
                customer.getId(),
                customer.getUser().getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getAddress()
        );
    }
}