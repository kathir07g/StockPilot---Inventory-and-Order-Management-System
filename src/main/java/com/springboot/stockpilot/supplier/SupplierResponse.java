package com.springboot.stockpilot.supplier;

import java.time.LocalDateTime;

public record SupplierResponse(

        Long id,
        String name,
        String email,
        String phone,
        String address,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {
}
