package com.springboot.stockpilot.catalog;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public Product createProduct(Product product){
        String sku=normalizeSku(product.getSku());

        if(productRepository.existsBySkuIgnoreCase(sku)){
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A product with this SKU already exists");
        }
        validateProduct(product);

        product.setSku(sku);
        product.setActive(true);

        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public Product getProductById(Long id){
        return productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Product not Found"));
    }

    @Transactional(readOnly = true)
    public List<Product> getAllActiveProducts(){
        return productRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional
    public Product updateProduct(Long id, Product updatedProduct){
        Product existingProduct = getProductById(id);

        String sku=normalizeSku(updatedProduct.getSku());
        productRepository.findBySkuIgnoreCase(sku)
                .filter(product -> !product.getId().equals(id))
                .ifPresent(product -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "A product with this SKU is already exists");
                });

        validateProduct(updatedProduct);
        existingProduct.setSku(sku);
        existingProduct.setName(updatedProduct.getName().trim());
        existingProduct.setDescription(updatedProduct.getDescription());
        existingProduct.setPrice(updatedProduct.getPrice());

        return productRepository.save(existingProduct);
    }

    @Transactional
    public void deactivateProduct(Long id){
        Product product=getProductById(id);
        product.setActive(false);

        productRepository.save(product);
    }

    private void validateProduct(Product product) {

        if(product.getName() == null || product.getName().isBlank()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Product name is required");
        }

        if(product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) < 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Product price must be zero or greater");
        }

        if(product.getSku() == null || product.getSku().isBlank()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Product SKU is required");
        }
    }

    private String normalizeSku(String sku) {
        if(sku == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Product SKU is required");
        }

        return sku.trim().toUpperCase(Locale.ROOT);
    }

}
