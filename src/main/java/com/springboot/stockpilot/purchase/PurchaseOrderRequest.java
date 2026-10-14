package com.springboot.stockpilot.purchase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record PurchaseOrderRequest(
        @NotNull(message = "Supplier ID is required")
        @Positive(message = "Supplier ID must be positive")
        Long supplierId,

        @NotEmpty(message = "Purchase order must contain at least one item")
        List<@Valid PurchaseOrderItemRequest> items

) {
}
