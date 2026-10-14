package com.springboot.stockpilot.purchase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReceivePurchaseOrderRequest(
        @NotEmpty(message = "At least one received item is required")
        List<@Valid ReceivePurchaseOrderItemRequest> items
) {
}
