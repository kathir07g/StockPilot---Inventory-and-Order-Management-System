
package com.springboot.stockpilot.customer;

import com.springboot.stockpilot.user.User;
import com.springboot.stockpilot.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomerServiceTest {

    private CustomerRepository customerRepository;
    private UserRepository userRepository;
    private CustomerService customerService;

    private Authentication authentication;
    private User user;
    private Customer customer;

    @BeforeEach
    void setUp() {
        customerRepository = mock(CustomerRepository.class);
        userRepository = mock(UserRepository.class);

        customerService = new CustomerService(customerRepository, userRepository);

        authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("CUSTOMER@example.com");

        user = new User();
        user.setName("Old Name");
        user.setEmail("customer@example.com");
        user.setActive(true);

        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", 1L);

        customer = new Customer();
        customer.setUser(user);
        customer.setName("Old Name");
        customer.setEmail("customer@example.com");
        customer.setPhone("1234567890");
        customer.setAddress("Old Address");
        customer.setActive(true);

        org.springframework.test.util.ReflectionTestUtils.setField(customer, "id", 10L);

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));

        when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(customer));
    }

    @Test
    void shouldReturnOwnProfile() {
        CustomerProfileResponse response = customerService.getMyProfile(authentication);

        assertEquals(10L, response.customerId());
        assertEquals(1L, response.userId());
        assertEquals("Old Name", response.name());
        assertEquals("customer@example.com", response.email());
        assertEquals("1234567890", response.phone());
        assertEquals("Old Address", response.address());
    }

    @Test
    void shouldUpdateProfileAndTrimValues() {
        UpdateCustomerProfileRequest request = new UpdateCustomerProfileRequest(
                        "  New Name  ",
                        " 9876543210 ",
                        " New Address ");

        CustomerProfileResponse response =
                customerService.updateMyProfile(request, authentication);

        assertEquals("New Name", response.name());
        assertEquals("9876543210", response.phone());
        assertEquals("New Address", response.address());

        assertEquals("New Name", user.getName());
        assertEquals("New Name", customer.getName());
    }

    @Test
    void shouldConvertBlankOptionalFieldsToNull() {
        UpdateCustomerProfileRequest request = new UpdateCustomerProfileRequest(
                        "New Name",
                        "   ",
                        "");

        CustomerProfileResponse response = customerService.updateMyProfile(request, authentication);

        assertNull(response.phone());
        assertNull(response.address());
    }

    @Test
    void shouldRejectInactiveUser() {
        user.setActive(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> customerService.getMyProfile(authentication));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(customerRepository, never()).findByUserId(anyLong());
    }

    @Test
    void shouldRejectInactiveCustomer() {
        customer.setActive(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> customerService.getMyProfile(authentication));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void shouldRejectMissingAuthentication() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> customerService.getMyProfile(null));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void shouldRejectUnknownUser() {
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> customerService.getMyProfile(authentication));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }
}
