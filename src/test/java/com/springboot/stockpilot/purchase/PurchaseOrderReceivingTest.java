
package com.springboot.stockpilot.purchase;

import com.springboot.stockpilot.catalog.Product;
import com.springboot.stockpilot.inventory.InventoryService;
import com.springboot.stockpilot.supplier.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class PurchaseOrderReceivingTest {

    private PurchaseOrderRepository purchaseOrderRepository;
    private PurchaseOrderItemRepository purchaseOrderItemRepository;
    private com.springboot.stockpilot.supplier.SupplierRepository supplierRepository;
    private com.springboot.stockpilot.catalog.ProductRepository productRepository;
    private InventoryService inventoryService;

    private PurchaseOrderService purchaseOrderService;

    private PurchaseOrder order;
    private PurchaseOrderItem item;
    private Product product;

    @BeforeEach
    void setUp() {
        purchaseOrderRepository = mock(PurchaseOrderRepository.class);
        purchaseOrderItemRepository = mock(PurchaseOrderItemRepository.class);
        supplierRepository = mock(com.springboot.stockpilot.supplier.SupplierRepository.class);
        productRepository = mock(com.springboot.stockpilot.catalog.ProductRepository.class);
        inventoryService = mock(InventoryService.class);

        purchaseOrderService = new PurchaseOrderService(
                purchaseOrderRepository,
                purchaseOrderItemRepository,
                supplierRepository,
                productRepository,
                inventoryService);

        Supplier supplier = new Supplier();
        ReflectionTestUtils.setField(supplier, "id", 10L);
        supplier.setName("Test Supplier");
        supplier.setActive(true);

        product = new Product();
        ReflectionTestUtils.setField(product, "id", 1L);
        product.setSku("TEST-001");
        product.setName("Test Product");
        product.setActive(true);

        order = new PurchaseOrder();
        ReflectionTestUtils.setField(order, "id", 100L);
        order.setOrderNumber("PO-TEST-100");
        order.setSupplier(supplier);
        order.setStatus("ORDERED");

        item = new PurchaseOrderItem();
        item.setProduct(product);
        item.setQuantityOrdered(10);
        item.setQuantityReceived(0);
        item.setUnitCost(new BigDecimal("25.00"));
        item.setLineTotal(new BigDecimal("250.00"));


        order.addItem(item);
        when(purchaseOrderRepository.findByIdForUpdate(100L))
                .thenReturn(Optional.of(order));
        when(purchaseOrderRepository.saveAndFlush(any(PurchaseOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldRejectReceivingMoreThanRemainingQuantity() {
        ReceivePurchaseOrderRequest request = new ReceivePurchaseOrderRequest(
                        List.of(new ReceivePurchaseOrderItemRequest(1L, 11)));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> purchaseOrderService.receivePurchaseOrder(100L, request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(0, item.getQuantityReceived());
        verify(inventoryService, never()).receiveStock(anyLong(), anyInt());
    }

    @Test
    void shouldRejectDuplicateProductIds() {
        ReceivePurchaseOrderRequest request = new ReceivePurchaseOrderRequest(
                        List.of(new ReceivePurchaseOrderItemRequest(1L, 2),
                                new ReceivePurchaseOrderItemRequest(1L, 3)));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> purchaseOrderService.receivePurchaseOrder(100L, request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(0, item.getQuantityReceived());
        verify(inventoryService, never()).receiveStock(anyLong(), anyInt());
    }

    @Test
    void shouldAcceptPartialReceivingAndUpdateStatus() {
        ReceivePurchaseOrderRequest request =
                new ReceivePurchaseOrderRequest(
                        List.of(new ReceivePurchaseOrderItemRequest(1L, 4)));

        PurchaseOrderResponse response =
                purchaseOrderService.receivePurchaseOrder(100L, request);

        assertEquals(4, item.getQuantityReceived());
        assertEquals("PARTIALLY_RECEIVED", order.getStatus());
        assertEquals("PARTIALLY_RECEIVED", response.status());

        verify(inventoryService).receiveStock(1L, 4);
        verify(purchaseOrderRepository).saveAndFlush(order);
    }

    @Test
    void shouldRejectNullReceivingItemWithBadRequest() {
        ReceivePurchaseOrderRequest request =
                new ReceivePurchaseOrderRequest(
                        java.util.Arrays.asList((ReceivePurchaseOrderItemRequest) null));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> purchaseOrderService.receivePurchaseOrder(100L, request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(inventoryService, never()).receiveStock(anyLong(), anyInt());
    }
}