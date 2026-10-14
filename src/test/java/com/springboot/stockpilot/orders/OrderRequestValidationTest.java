package com.springboot.stockpilot.orders;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    void shouldRejectOrderWithNoItems() {
        OrderRequest request = new OrderRequest(List.of());

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectInvalidProductIdAndQuantity() {
        OrderRequest request = new OrderRequest(List.of(new OrderItemRequest(-1L, 0)));

        Set<String> invalidFields = validator.validate(request)
                .stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertTrue(invalidFields.contains("items[0].productId"));
        assertTrue(invalidFields.contains("items[0].quantity"));
    }

    @Test
    void shouldAcceptValidOrderRequest() {
        OrderRequest request = new OrderRequest(
                List.of(new OrderItemRequest(1L, 2),
                        new OrderItemRequest(2L, 1)));

        assertTrue(validator.validate(request).isEmpty());
    }
}