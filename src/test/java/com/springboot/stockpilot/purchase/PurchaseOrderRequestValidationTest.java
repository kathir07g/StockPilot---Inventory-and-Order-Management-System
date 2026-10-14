package com.springboot.stockpilot.purchase;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PurchaseOrderRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void shouldRejectMissingSupplier() {
        PurchaseOrderRequest request = new PurchaseOrderRequest(
                null,
                List.of(new PurchaseOrderItemRequest(
                        1L, 2, new BigDecimal("10.00")
                ))
        );

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectEmptyItems() {
        PurchaseOrderRequest request = new PurchaseOrderRequest(
                1L,
                List.of()
        );

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldAcceptValidPurchaseOrderRequest() {
        PurchaseOrderRequest request = new PurchaseOrderRequest(
                1L,
                List.of(new PurchaseOrderItemRequest(
                        1L, 2, new BigDecimal("10.00")
                ))
        );

        assertTrue(validator.validate(request).isEmpty());
    }
}