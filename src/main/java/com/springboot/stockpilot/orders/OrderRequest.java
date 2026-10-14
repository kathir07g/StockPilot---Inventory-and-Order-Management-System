package com.springboot.stockpilot.orders;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OrderRequest (
        @NotEmpty(message = "Order must contain at least one item")
        List<@Valid OrderItemRequest> items
){
}
