package com.springboot.stockpilot.customer;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/me")
    public CustomerProfileResponse getMyProfile(Authentication authentication) {
        return customerService.getMyProfile(authentication);
    }

    @PutMapping("/me")
    public CustomerProfileResponse updateMyProfile(@Valid @RequestBody UpdateCustomerProfileRequest request,
            Authentication authentication) {

        return customerService.updateMyProfile(request, authentication);
    }
}