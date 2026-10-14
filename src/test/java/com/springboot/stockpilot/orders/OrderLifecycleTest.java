package com.springboot.stockpilot.orders;

import com.springboot.stockpilot.catalog.Product;
import com.springboot.stockpilot.catalog.ProductRepository;
import com.springboot.stockpilot.customer.Customer;
import com.springboot.stockpilot.customer.CustomerRepository;
import com.springboot.stockpilot.inventory.Inventory;
import com.springboot.stockpilot.inventory.InventoryRepository;
import com.springboot.stockpilot.user.User;
import com.springboot.stockpilot.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderLifecycleTest {

    private OrderRepository orderRepository;
    private ProductRepository productRepository;
    private InventoryRepository inventoryRepository;
    private CustomerRepository customerRepository;
    private UserRepository userRepository;
    private OrderService orderService;

    private User user;
    private Customer customer;
    private Authentication authentication;

    private Order order;
    private Product product;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        productRepository = mock(ProductRepository.class);
        inventoryRepository = mock(InventoryRepository.class);
        customerRepository = mock(CustomerRepository.class);
        userRepository = mock(UserRepository.class);

        orderService = new OrderService(
                orderRepository,
                productRepository,
                inventoryRepository,
                customerRepository,
                userRepository);

        authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("customer@example.com");

        user = new User();
        ReflectionTestUtils.setField(user, "id", 1L);
        user.setActive(true);

        customer = new Customer();
        ReflectionTestUtils.setField(customer, "id", 10L);
        customer.setActive(true);

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));

        when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(customer));

        product = new Product();
        ReflectionTestUtils.setField(product, "id", 100L);
        product.setSku("TEST-100");
        product.setName("Test Product");
        product.setPrice(new BigDecimal("50.00"));
        product.setActive(true);

        inventory = new Inventory();
        inventory.setProduct(product);
        inventory.setQuantityOnHand(20);
        inventory.setQuantityReserved(5);

        order = new Order();
        ReflectionTestUtils.setField(order, "id", 500L);
        order.setOrderNumber("TEST-ORDER-500");
        order.setCustomer(customer);
        order.setStatus("PLACED");
        order.setTotalAmount(new BigDecimal("100.00"));

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("50.00"));
        item.setLineTotal(new BigDecimal("100.00"));
        order.addItem(item);
    }

    @Test
    void shouldCancelOrderAndReleaseReservedStock() {
        when(orderRepository.findByIdAndCustomerIdForUpdate(500L, 10L)).thenReturn(Optional.of(order));

        when(inventoryRepository.findByProductIdForUpdate(100L)).thenReturn(Optional.of(inventory));

        OrderResponse response = orderService.cancelOrder(500L, authentication);

        assertEquals("CANCELLED", response.status());
        assertEquals("CANCELLED", order.getStatus());
        assertEquals(20, inventory.getQuantityOnHand());
        assertEquals(3, inventory.getQuantityReserved());
    }

    @Test
    void shouldFulfillOrderAndReduceStock() {
        when(orderRepository.findByIdForUpdate(500L)).thenReturn(Optional.of(order));

        when(inventoryRepository.findByProductIdForUpdate(100L)).thenReturn(Optional.of(inventory));

        OrderResponse response = orderService.fulfillOrder(500L);

        assertEquals("FULFILLED", response.status());
        assertEquals("FULFILLED", order.getStatus());
        assertEquals(18, inventory.getQuantityOnHand());
        assertEquals(3, inventory.getQuantityReserved());
    }

    @Test
    void shouldRejectRepeatedFulfillment() {
        order.setStatus("FULFILLED");

        when(orderRepository.findByIdForUpdate(500L)).thenReturn(Optional.of(order));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> orderService.fulfillOrder(500L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(20, inventory.getQuantityOnHand());
        assertEquals(5, inventory.getQuantityReserved());

        verifyNoInteractions(inventoryRepository);
    }

    @Test
    void shouldNotChangeAnyInventoryWhenOneItemHasInsufficientStock() {
        Product secondProduct = new Product();
        ReflectionTestUtils.setField(secondProduct, "id", 200L);
        secondProduct.setSku("TEST-200");
        secondProduct.setName("Second Product");
        secondProduct.setPrice(new BigDecimal("25.00"));
        secondProduct.setActive(true);

        OrderItem secondItem = new OrderItem();
        secondItem.setProduct(secondProduct);
        secondItem.setQuantity(4);
        secondItem.setUnitPrice(new BigDecimal("25.00"));
        secondItem.setLineTotal(new BigDecimal("100.00"));
        order.addItem(secondItem);

        Inventory secondInventory = new Inventory();
        secondInventory.setProduct(secondProduct);
        secondInventory.setQuantityOnHand(3);
        secondInventory.setQuantityReserved(0);

        when(orderRepository.findByIdForUpdate(500L)).thenReturn(Optional.of(order));

        when(inventoryRepository.findByProductIdForUpdate(100L)).thenReturn(Optional.of(inventory));

        when(inventoryRepository.findByProductIdForUpdate(200L)).thenReturn(Optional.of(secondInventory));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> orderService.fulfillOrder(500L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        assertEquals(20, inventory.getQuantityOnHand());
        assertEquals(5, inventory.getQuantityReserved());

        assertEquals(3, secondInventory.getQuantityOnHand());
        assertEquals(0, secondInventory.getQuantityReserved());
    }
}