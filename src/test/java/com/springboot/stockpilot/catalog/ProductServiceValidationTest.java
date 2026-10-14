
package com.springboot.stockpilot.catalog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductServiceValidationTest {

    private ProductRepository productRepository;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        productService = new ProductService(productRepository);
    }

    @Test
    void shouldRejectProductWithBlankName() {
        Product product = validProduct();
        product.setName(" ");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> productService.createProduct(product));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldRejectNegativePrice() {
        Product product = validProduct();
        product.setPrice(new BigDecimal("-1.00"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> productService.createProduct(product));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldRejectMissingPrice() {
        Product product = validProduct();
        product.setPrice(null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> productService.createProduct(product));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldRejectDuplicateSku() {
        Product product = validProduct();

        when(productRepository.existsBySkuIgnoreCase("TEST-001")).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> productService.createProduct(product));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(productRepository, never()).save(any(Product.class));
    }

    private Product validProduct() {
        Product product = new Product();
        product.setSku("test-001");
        product.setName("Test Product");
        product.setDescription("Test description");
        product.setPrice(new BigDecimal("100.00"));
        return product;
    }
}