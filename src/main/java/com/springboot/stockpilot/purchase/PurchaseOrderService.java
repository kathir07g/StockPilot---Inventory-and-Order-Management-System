package com.springboot.stockpilot.purchase;

import com.springboot.stockpilot.catalog.Product;
import com.springboot.stockpilot.catalog.ProductRepository;
import com.springboot.stockpilot.inventory.InventoryService;
import com.springboot.stockpilot.supplier.Supplier;
import com.springboot.stockpilot.supplier.SupplierRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    @PersistenceContext
    private EntityManager entityManager;

    public PurchaseOrderService(
            PurchaseOrderRepository purchaseOrderRepository,
            PurchaseOrderItemRepository purchaseOrderItemRepository,
            SupplierRepository supplierRepository,
            ProductRepository productRepository,
            InventoryService inventoryService) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseOrderItemRepository = purchaseOrderItemRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional
    public PurchaseOrderResponse createPurchaseOrder(PurchaseOrderRequest request) {
        if (request == null || request.supplierId() == null
                || request.items() == null || request.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Supplier and at least one purchase item are required");
        }

        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Supplier not found"));

        if (!supplier.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot create a purchase order for an inactive supplier");
        }

        Set<Long> productIds = new HashSet<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setOrderNumber("PO-" + UUID.randomUUID().toString()
                        .replace("-", "").substring(0, 12).toUpperCase());
        purchaseOrder.setSupplier(supplier);
        purchaseOrder.setStatus("ORDERED");
        purchaseOrder.setTotalAmount(BigDecimal.ZERO);

        for (PurchaseOrderItemRequest itemRequest : request.items()) {
            if (itemRequest == null || itemRequest.productId() == null
                    || itemRequest.quantityOrdered() == null || itemRequest.unitCost() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Each item must contain productId, quantityOrdered and unitCost");
            }

            Long productId = itemRequest.productId();
            Integer quantity = itemRequest.quantityOrdered();
            BigDecimal unitCost = itemRequest.unitCost();

            if (!productIds.add(productId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Duplicate product IDs are not allowed in one purchase order");
            }

            if (quantity <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Ordered quantity must be greater than zero");
            }

            if (unitCost.compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Unit cost cannot be negative");
            }

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Product not found: " + productId));

            if (!product.isActive()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot order inactive product: " + productId);
            }

            try {
                unitCost = unitCost.setScale(2, RoundingMode.UNNECESSARY);
            } catch (ArithmeticException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Unit cost cannot have more than two decimal places");
            }

            BigDecimal lineTotal = unitCost.multiply(BigDecimal.valueOf(quantity));

            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setProduct(product);
            item.setQuantityOrdered(quantity);
            item.setQuantityReceived(0);
            item.setUnitCost(unitCost);
            item.setLineTotal(lineTotal);

            purchaseOrder.addItem(item);
            totalAmount = totalAmount.add(lineTotal);
        }
        purchaseOrder.setTotalAmount(totalAmount);

        PurchaseOrder savedOrder = purchaseOrderRepository.saveAndFlush(purchaseOrder);
        entityManager.refresh(savedOrder);
        return toResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponse getPurchaseOrderById(Long id) {
        PurchaseOrder order = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Purchase order not found: " + id));

        return toResponse(order);
    }


    @Transactional(readOnly = true)
    public Page<PurchaseOrderResponse> getAllPurchaseOrders(Pageable pageable) {
        return purchaseOrderRepository.findAll(pageable)
                .map(this::toResponse);
    }


    @Transactional
    public PurchaseOrderResponse receivePurchaseOrder(Long purchaseOrderId,
            ReceivePurchaseOrderRequest request) {

        if (purchaseOrderId == null || purchaseOrderId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Purchase order ID must be greater than zero");
        }

        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "At least one received item is required");
        }

        if (request.items().stream().anyMatch(Objects::isNull)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Received items cannot contain null entries");
        }

        for (ReceivePurchaseOrderItemRequest received : request.items()) {
            if (received.productId() == null || received.productId() <= 0
                    || received.quantityReceived() == null || received.quantityReceived() <= 0) {

                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Each item needs a valid product ID and a positive received quantity"
                );
            }
        }

        List<ReceivePurchaseOrderItemRequest> sortedReceivedItems =
                request.items().stream()
                        .sorted(Comparator.comparing(
                                ReceivePurchaseOrderItemRequest::productId))
                        .toList();

        Set<Long> receivedProductIds = new HashSet<>();

        PurchaseOrder order = purchaseOrderRepository.findByIdForUpdate(purchaseOrderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Purchase order not found"));

        if (!"ORDERED".equals(order.getStatus()) && !"PARTIALLY_RECEIVED".equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Purchase order cannot receive stock in its current status");
        }

        for (ReceivePurchaseOrderItemRequest received : sortedReceivedItems) {

            if (!receivedProductIds.add(received.productId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Duplicate product IDs are not allowed in one receiving request");
            }

            PurchaseOrderItem item = order.getItems().stream()
                    .filter(existing ->
                            existing.getProduct().getId()
                                    .equals(received.productId()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Product " + received.productId() + " does not belong to this purchase order"));

            int remaining = item.getQuantityOrdered() - item.getQuantityReceived();

            if (received.quantityReceived() > remaining) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Received quantity exceeds the remaining quantity for product " + received.productId());
            }
        }

        for (ReceivePurchaseOrderItemRequest received : sortedReceivedItems) {

            PurchaseOrderItem item = order.getItems().stream()
                    .filter(existing ->
                            existing.getProduct().getId()
                                    .equals(received.productId()))
                    .findFirst()
                    .orElseThrow();

            inventoryService.receiveStock(received.productId(), received.quantityReceived());

            item.setQuantityReceived(Math.addExact(item.getQuantityReceived(),
                            received.quantityReceived()));
        }

        boolean fullyReceived = order.getItems().stream()
                .allMatch(item ->
                        item.getQuantityReceived()
                                .equals(item.getQuantityOrdered()));

        order.setStatus(fullyReceived ? "RECEIVED" : "PARTIALLY_RECEIVED");

        PurchaseOrder savedOrder = purchaseOrderRepository.saveAndFlush(order);

        return toResponse(savedOrder);
    }


    @Transactional
    public PurchaseOrderResponse cancelPurchaseOrder(Long purchaseOrderId) {
        if (purchaseOrderId == null || purchaseOrderId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Purchase order ID must be greater than zero");
        }

        PurchaseOrder order = purchaseOrderRepository.findByIdForUpdate(purchaseOrderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Purchase order not found: " + purchaseOrderId));

        if (!"ORDERED".equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only ORDERED purchase orders can be cancelled");
        }
        boolean stockAlreadyReceived = order.getItems().stream()
                .anyMatch(item -> item.getQuantityReceived() != null
                        && item.getQuantityReceived() > 0);

        if (stockAlreadyReceived) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A purchase order with received stock cannot be cancelled");
        }
        order.setStatus("CANCELLED");
        PurchaseOrder savedOrder = purchaseOrderRepository.saveAndFlush(order);

        return toResponse(savedOrder);
    }

    private PurchaseOrderResponse toResponse(PurchaseOrder order) {

        var itemResponses = order.getItems().stream()
                .map(item -> new PurchaseOrderItemResponse(
                        item.getProduct().getId(),
                        item.getProduct().getSku(),
                        item.getProduct().getName(),
                        item.getQuantityOrdered(),
                        item.getQuantityReceived(),
                        item.getUnitCost(),
                        item.getLineTotal()))
                .toList();

        return new PurchaseOrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getSupplier().getId(),
                order.getSupplier().getName(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                itemResponses);
    }
}