package com.springboot.stockpilot.orders;

import com.springboot.stockpilot.catalog.ProductRepository;
import com.springboot.stockpilot.customer.Customer;
import com.springboot.stockpilot.customer.CustomerRepository;
import com.springboot.stockpilot.inventory.InventoryRepository;
import com.springboot.stockpilot.user.User;
import com.springboot.stockpilot.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class OrderOwnershipTest {

    private OrderRepository orderRepository;
    private CustomerRepository customerRepository;
    private UserRepository userRepository;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        customerRepository = mock(CustomerRepository.class);
        userRepository = mock(UserRepository.class);

        orderService = new OrderService(
                orderRepository,
                mock(ProductRepository.class),
                mock(InventoryRepository.class),
                customerRepository,
                userRepository);
    }

    @Test
    void shouldNotReturnAnotherCustomersOrder() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("customer@example.com");

        User user = new User();
        user.setActive(true);

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));

        Customer customer = new Customer();
        customer.setActive(true);

        when(customerRepository.findByUserId(user.getId())).thenReturn(Optional.of(customer));

        when(orderRepository.findByIdAndCustomer_Id(100L, customer.getId())).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> orderService.getOrderById(100L, authentication));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(orderRepository).findByIdAndCustomer_Id(100L, customer.getId());
    }
}