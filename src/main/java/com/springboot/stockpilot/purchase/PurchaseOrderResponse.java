package com.springboot.stockpilot.purchase;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PurchaseOrderResponse(
        Long id,
        String orderNumber,
        Long supplierId,
        String supplierName,
        String status,
        BigDecimal totalAmount,
        LocalDateTime createdAt,
        List<PurchaseOrderItemResponse> items

) {
}
