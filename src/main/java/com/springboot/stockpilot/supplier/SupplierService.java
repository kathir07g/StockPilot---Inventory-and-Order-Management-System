package com.springboot.stockpilot.supplier;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Transactional
    public SupplierResponse createSupplier(SupplierRequest request){
        String email=normalizeEmail(request.email());

        if(email!=null && supplierRepository.existsByEmailIgnoreCase(email)){
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Supplier email already exists");
        }
        Supplier supplier=new Supplier();
        applyRequest(supplier, request, email);
        supplier.setActive(true);

        return toResponse(supplierRepository.saveAndFlush(supplier));
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> getActiveSuppliers(){
        return supplierRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SupplierResponse getSupplierById(Long id){
        Supplier supplier=supplierRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Supplier not found"));

        return toResponse(supplier);
    }

    @Transactional
    public SupplierResponse updateSupplier(Long id, SupplierRequest request){
        Supplier supplier=supplierRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Supplier not found"));

        if(!supplier.isActive()){
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Inactive supplier cannot be update");
        }

        String email=normalizeEmail(request.email());

        if(email!=null){
            supplierRepository.findByEmailIgnoreCase(email)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "Supplier email already exists");
                    });
        }
        applyRequest(supplier, request, email);

        return toResponse(supplierRepository.saveAndFlush(supplier));
    }

    @Transactional
    public void deactivateSupplier(Long id){
        Supplier supplier=supplierRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Supplier not found"));
        supplier.setActive(false);
        supplierRepository.save(supplier);
    }

    private void applyRequest(Supplier supplier, SupplierRequest request, String email) {
        supplier.setName(request.name().trim());
        supplier.setEmail(email);
        supplier.setPhone(normalizeOptional(request.phone()));
        supplier.setAddress(normalizeOptional(request.address()));
    }

    private String normalizeEmail(String email) {
        if(email==null || email.isBlank()){
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        if(value==null || value.isBlank()){
            return null;
        }
        return value.trim();
    }

    public SupplierResponse toResponse(Supplier supplier){
        return new SupplierResponse(
                supplier.getId(),
                supplier.getName(),
                supplier.getEmail(),
                supplier.getPhone(),
                supplier.getAddress(),
                supplier.isActive(),
                supplier.getCreatedAt(),
                supplier.getUpdatedAt()
        );
    }

}
