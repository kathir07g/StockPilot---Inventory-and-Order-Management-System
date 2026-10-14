
package com.springboot.stockpilot.inventory;

import java.time.LocalDateTime;

public record InventoryResponse(
        Long inventoryId,
        Long productId,
        String sku,
        String productName,
        int quantityOnHand,
        int quantityReserved,
        int availableQuantity,
        int reorderLevel,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
