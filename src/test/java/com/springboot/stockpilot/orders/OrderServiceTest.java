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
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    private OrderRepository orderRepository;
    private ProductRepository productRepository;
    private InventoryRepository inventoryRepository;
    private CustomerRepository customerRepository;
    private UserRepository userRepository;

    private OrderService orderService;

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
                userRepository
        );
    }

    @Test
    void shouldRejectOrderWhenAvailableStockIsInsufficient() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("customer@example.com");

        User user = new User();
        user.setActive(true);

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));

        Customer customer = new Customer();
        customer.setActive(true);

        when(customerRepository.findByUserId(user.getId())).thenReturn(Optional.of(customer));

        Product product = new Product();
        product.setSku("TEST-001");
        product.setName("Test Product");
        product.setPrice(new BigDecimal("100.00"));
        product.setActive(true);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        Inventory inventory = new Inventory();
        inventory.setProduct(product);
        inventory.setQuantityOnHand(5);
        inventory.setQuantityReserved(4);

        when(inventoryRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.of(inventory));

        OrderRequest request = new OrderRequest(List.of(new OrderItemRequest(1L, 2)));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> orderService.placeOrder(request, authentication)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        assertEquals(5, inventory.getQuantityOnHand());
        assertEquals(4, inventory.getQuantityReserved());

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void shouldRejectOrderWhenUserIsInactive() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("customer@example.com");

        User user = new User();
        user.setActive(false);

        when(userRepository.findByEmail("customer@example.com"))
                .thenReturn(Optional.of(user));

        OrderRequest request = new OrderRequest(
                List.of(new OrderItemRequest(1L, 1))
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> orderService.placeOrder(request, authentication)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());

        verifyNoInteractions(productRepository, inventoryRepository);
        verify(orderRepository, never()).save(any(Order.class));
    }

}
