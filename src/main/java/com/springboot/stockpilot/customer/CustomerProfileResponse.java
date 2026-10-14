package com.springboot.stockpilot.customer;

public record CustomerProfileResponse(
        Long customerId,
        Long userId,
        String name,
        String email,
        String phone,
        String address
) {
}