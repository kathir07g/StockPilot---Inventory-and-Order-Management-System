
package com.springboot.stockpilot.inventory;

import com.springboot.stockpilot.catalog.Product;
import com.springboot.stockpilot.catalog.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InventoryServiceValidationTest {

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
    void shouldRejectZeroReceivedQuantity() {
        assertStatus(HttpStatus.BAD_REQUEST,
                () -> inventoryService.receiveStock(1L, 0));

        verifyNoInteractions(productRepository, inventoryRepository);
    }

    @Test
    void shouldRejectNegativeReceivedQuantity() {
        assertStatus(HttpStatus.BAD_REQUEST,
                () -> inventoryService.receiveStock(1L, -5));

        verifyNoInteractions(productRepository, inventoryRepository);
    }

    @Test
    void shouldRejectReceivingStockForInactiveProduct() {
        product.setActive(false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertStatus(HttpStatus.BAD_REQUEST,
                () -> inventoryService.receiveStock(1L, 5));

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void shouldRejectStockOverflow() {
        inventory.setQuantityOnHand(Integer.MAX_VALUE);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.of(inventory));

        assertStatus(HttpStatus.BAD_REQUEST,
                () -> inventoryService.receiveStock(1L, 1));

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void shouldRejectNegativeReorderLevel() {
        assertStatus(HttpStatus.BAD_REQUEST,
                () -> inventoryService.updateReorderLevel(1L, -1));

        verifyNoInteractions(inventoryRepository);
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingMissingInventory() {
        when(inventoryRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.empty());

        assertStatus(HttpStatus.NOT_FOUND,
                () -> inventoryService.updateReorderLevel(1L, 5));

        verify(inventoryRepository, never()).save(any());
    }

    private void assertStatus(HttpStatus expected,
                              org.junit.jupiter.api.function.Executable action) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, action);

        assertEquals(expected, exception.getStatusCode());
    }
}