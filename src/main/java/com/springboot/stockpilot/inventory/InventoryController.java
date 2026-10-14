package com.springboot.stockpilot.inventory;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/{productId}/receive")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER')")
    public InventoryResponse receiveStock(@PathVariable Long productId,
                                  @RequestParam int quantity){
        return inventoryService.receiveStock(productId, quantity);
    }

    @GetMapping("/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','SALES_STAFF')")
    public InventoryResponse getInventory(@PathVariable Long productId){
        return inventoryService.getInventory(productId);
    }

    @PutMapping("/{productId}/reorder-level")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER')")
    public InventoryResponse updateReorderLevel(@PathVariable Long productId,
                                        @RequestParam int quantity){
        return inventoryService.updateReorderLevel(productId, quantity);
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','SALES_STAFF')")
    public List<InventoryResponse> getLowStockInventory(){
        return inventoryService.getLowStockInventory();
    }
}
