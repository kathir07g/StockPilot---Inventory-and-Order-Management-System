package com.springboot.stockpilot.orders;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public OrderResponse placeOrder(@Valid @RequestBody OrderRequest request,
                                    Authentication authentication){
        return orderService.placeOrder(request, authentication);
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public OrderResponse getOrderById(
            @PathVariable Long orderId,
            Authentication authentication) {

        return orderService.getOrderById(orderId, authentication);
    }

    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public OrderResponse cancelOrder(
            @PathVariable Long orderId,
            Authentication authentication) {

        return orderService.cancelOrder(orderId, authentication);
    }

    @PostMapping("/{orderId}/fulfill")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER')")
    public OrderResponse fulfillOrder(
            @PathVariable Long orderId) {

        return orderService.fulfillOrder(orderId);
    }


    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<OrderResponse> getMyOrders(
            Authentication authentication) {

        return orderService.getMyOrders(authentication);
    }


    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER', 'SALES_STAFF')")
    public Page<OrderResponse> getAllOrdersForStaff(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and size must be between 1 and 100");
        }

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt")
                        .and(Sort.by(Sort.Direction.DESC, "id")));

        return orderService.getAllOrdersForStaff(pageable);
    }


}
