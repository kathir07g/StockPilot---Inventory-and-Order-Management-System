package com.springboot.stockpilot.purchase;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReceivePurchaseOrderItemRequest(
        @NotNull(message = "Product ID is required")
        @Positive(message = "Product ID must be positive")
        Long productId,

        @NotNull(message = "Received quantity is required")
        @Positive(message = "Received quantity must be greater than zero")
        Integer quantityReceived
) {
}
