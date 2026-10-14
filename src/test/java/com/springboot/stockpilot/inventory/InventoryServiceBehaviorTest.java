package com.springboot.stockpilot.inventory;

import com.springboot.stockpilot.catalog.Product;
import com.springboot.stockpilot.catalog.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InventoryServiceBehaviorTest {

    private InventoryRepository inventoryRepository;
    private ProductRepository productRepository;
    private InventoryService inventoryService;
    private Product product;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        inventoryRepository = mock(InventoryRepository.class);
        productRepository = mock(ProductRepository.class);
        inventoryService = new InventoryService(inventoryRepository, productRepository);

        product = new Product();
        ReflectionTestUtils.setField(product, "id", 1L);
        product.setSku("SKU-001");
        product.setName("Test Product");
        product.setActive(true);

        inventory = new Inventory();
        ReflectionTestUtils.setField(inventory, "Id", 10L);
        inventory.setProduct(product);
        inventory.setQuantityOnHand(20);
        inventory.setQuantityReserved(5);
        inventory.setReorderLevel(10);
    }

    @Test
    void shouldReceiveStockAndPreserveReservedQuantity() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InventoryResponse response = inventoryService.receiveStock(1L, 8);

        assertEquals(28, inventory.getQuantityOnHand());
        assertEquals(5, inventory.getQuantityReserved());
        assertEquals(23, inventory.getAvailableQuantity());
        assertEquals(28, response.quantityOnHand());
        assertEquals(5, response.quantityReserved());
        assertEquals(23, response.availableQuantity());

        verify(inventoryRepository).save(inventory);
    }

    @Test
    void shouldCreateInventoryWhenReceivingStockForFirstTime() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.empty());
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InventoryResponse response = inventoryService.receiveStock(1L, 12);

        assertEquals(12, response.quantityOnHand());
        assertEquals(0, response.quantityReserved());
        assertEquals(12, response.availableQuantity());
        assertEquals(10, response.reorderLevel());

        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    void shouldIncludeInventoryAtReorderThresholdInLowStock() {
        Inventory atThreshold = new Inventory();
        atThreshold.setProduct(product);
        atThreshold.setQuantityOnHand(15);
        atThreshold.setQuantityReserved(5);
        atThreshold.setReorderLevel(10);

        when(inventoryRepository.findAll()).thenReturn(List.of(atThreshold));

        List<InventoryResponse> result = inventoryService.getLowStockInventory();

        assertEquals(1, result.size());
        assertEquals(10, result.get(0).availableQuantity());
    }

    @Test
    void shouldExcludeInventoryAboveReorderThreshold() {
        inventory.setQuantityOnHand(30);
        inventory.setQuantityReserved(5);
        inventory.setReorderLevel(10);

        when(inventoryRepository.findAll()).thenReturn(List.of(inventory));

        List<InventoryResponse> result = inventoryService.getLowStockInventory();

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldIncludeInventoryWhenReservationsCauseLowAvailableStock() {
        inventory.setQuantityOnHand(20);
        inventory.setQuantityReserved(12);
        inventory.setReorderLevel(10);

        when(inventoryRepository.findAll()).thenReturn(List.of(inventory));

        List<InventoryResponse> result = inventoryService.getLowStockInventory();

        assertEquals(1, result.size());
        assertEquals(8, result.get(0).availableQuantity());
    }
}