package com.springboot.stockpilot.orders;

import java.math.BigDecimal;
import java.util.List;

public record OrderResponse(
        Long OrderId,
        String OrderNumber,
        String status,
        BigDecimal totalAmount,
        List<OrderItemResponse> items
) {
}
