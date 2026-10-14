package com.springboot.stockpilot.inventory;

import com.springboot.stockpilot.catalog.Product;
import com.springboot.stockpilot.catalog.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    public InventoryService(InventoryRepository inventoryRepository,
                            ProductRepository productRepository) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public InventoryResponse receiveStock(Long productId, int quantity){
        if(quantity<=0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Received quantity must greater than zero");
        }

        Product product=productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Product not found"));

        if(!product.isActive()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot receive stock for an inactive product");
        }

        Inventory inventory=inventoryRepository.findByProductIdForUpdate(productId)
                .orElseGet(() -> {Inventory newInventory = new Inventory();
                    newInventory.setProduct(product);
                    newInventory.setReorderLevel(10);
                    return newInventory;
                });

        try {
            inventory.setQuantityOnHand(Math.addExact(inventory.getQuantityOnHand(), quantity));
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Received quantity exceeds the maximum supported stock");
        }

        Inventory saved = inventoryRepository.save(inventory);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventory(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Inventory record not found for this product"));
        return toResponse(inventory);
    }

    @Transactional
    public InventoryResponse updateReorderLevel(Long productId, int reorderLevel){
        if(reorderLevel<0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Reorder level cannot be negative");
        }

        Inventory inventory=inventoryRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Inventory record not found for this product"));

        inventory.setReorderLevel(reorderLevel);

        Inventory saved = inventoryRepository.save(inventory);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getLowStockInventory() {
        return inventoryRepository.findAll()
                .stream()
                .filter(inventory ->
                        inventory.getAvailableQuantity()
                                <= inventory.getReorderLevel())
                .map(this::toResponse)
                .toList();
    }

    private InventoryResponse toResponse(Inventory inventory) {
        Product product = inventory.getProduct();

        return new InventoryResponse(
                inventory.getId(),
                product.getId(),
                product.getSku(),
                product.getName(),
                inventory.getQuantityOnHand(),
                inventory.getQuantityReserved(),
                inventory.getAvailableQuantity(),
                inventory.getReorderLevel(),
                inventory.getCreatedAt(),
                inventory.getUpdatedAt()
        );
    }

}
