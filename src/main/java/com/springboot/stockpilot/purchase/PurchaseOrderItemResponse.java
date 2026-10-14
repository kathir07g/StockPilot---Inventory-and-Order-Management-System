package com.springboot.stockpilot.purchase;

import java.math.BigDecimal;

public record PurchaseOrderItemResponse(
        Long productId,
        String sku,
        String productName,
        Integer quantityOrdered,
        Integer quantityReceived,
        BigDecimal unitCost,
        BigDecimal lineTotal

) {
}
