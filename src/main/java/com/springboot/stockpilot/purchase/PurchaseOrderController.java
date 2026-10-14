package com.springboot.stockpilot.purchase;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/purchase-orders")
@PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER')")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(
            PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseOrderResponse createPurchaseOrder(
            @Valid @RequestBody PurchaseOrderRequest request) {

        return purchaseOrderService.createPurchaseOrder(request);
    }

    @GetMapping("/{id}")
    public PurchaseOrderResponse getPurchaseOrderById(
            @PathVariable Long id) {
        return purchaseOrderService.getPurchaseOrderById(id);
    }


    @GetMapping
    public Page<PurchaseOrderResponse> getAllPurchaseOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and size must be between 1 and 100");
        }

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt")
                        .and(Sort.by(Sort.Direction.DESC, "id")));

        return purchaseOrderService.getAllPurchaseOrders(pageable);
    }



    @PostMapping("/{id}/receive")
    public PurchaseOrderResponse receivePurchaseOrder(
            @PathVariable Long id,
            @Valid @RequestBody ReceivePurchaseOrderRequest request) {

        return purchaseOrderService.receivePurchaseOrder(id, request);
    }

    @PostMapping("/{id}/cancel")
    public PurchaseOrderResponse cancelPurchaseOrder(
            @PathVariable Long id) {

        return purchaseOrderService.cancelPurchaseOrder(id);
    }


}