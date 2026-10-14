package com.springboot.stockpilot.orders;

import com.springboot.stockpilot.catalog.Product;
import com.springboot.stockpilot.catalog.ProductRepository;
import com.springboot.stockpilot.customer.Customer;
import com.springboot.stockpilot.customer.CustomerRepository;
import com.springboot.stockpilot.inventory.Inventory;
import com.springboot.stockpilot.inventory.InventoryRepository;
import com.springboot.stockpilot.user.User;
import com.springboot.stockpilot.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository,
                        InventoryRepository inventoryRepository, CustomerRepository customerRepository, UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public OrderResponse placeOrder(OrderRequest request,
                                    Authentication authentication){

        User user=userRepository.findByEmail(authentication.getName().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "User not found"));

        Customer customer=customerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Customer account not found"));

        if (!user.isActive() || !customer.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Inactive accounts cannot place orders");
        }

        Map<Long, Integer> requestedItems=new LinkedHashMap<>();
        for(OrderItemRequest item: request.items()){
            if(item.productId()==null || item.productId()<=0 || item.quantity() == null
            || item.quantity()<=0){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Product ID and quantity must be positive");
            }

            try{
                requestedItems.merge(item.productId(), item.quantity(), Math::addExact);
            }
            catch(ArithmeticException exception){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Total requested quantity is too large");
            }
        }

        List<Long> productIds=requestedItems.keySet().stream().sorted().toList();

        Map<Long, Product> products=new LinkedHashMap<>();
        Map<Long, Inventory> inventories=new LinkedHashMap<>();

        for(Long productId:productIds){
            Product product=productRepository.findById(productId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Product not found: "+productId));

            if(!product.isActive()){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Product is inactive: "+productId);
            }

            Inventory inventory=inventoryRepository.findByProductIdForUpdate(productId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Inventory not available for product: "+productId));

            int quantity=requestedItems.get(productId);
            int available=inventory.getAvailableQuantity();

            if(available<quantity){
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Insufficient stock for product: "+productId);
            }
            products.put(productId, product);
            inventories.put(productId, inventory);
        }

        Order order=new Order();
        order.setOrderNumber(UUID.randomUUID().toString()
                .replace("-","").substring(0,20)
                .toUpperCase());
        order.setCustomer(customer);
        order.setStatus("PLACED");
        order.setTotalAmount(BigDecimal.ZERO);
        BigDecimal totalAmount=BigDecimal.ZERO;

        for(Long productId: productIds){
            Product product=products.get(productId);
            Inventory inventory=inventories.get(productId);
            int quantity=requestedItems.get(productId);

            BigDecimal unitPrice=product.getPrice();
            BigDecimal lineTotal=unitPrice.multiply(
                    BigDecimal.valueOf(quantity));

            OrderItem orderItem=new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(quantity);
            orderItem.setUnitPrice(unitPrice);
            orderItem.setLineTotal(lineTotal);

            order.addItem(orderItem);

            inventory.setQuantityReserved(
                    Math.addExact(inventory.getQuantityReserved(),quantity));

            totalAmount=totalAmount.add(lineTotal);
        }
        order.setTotalAmount(totalAmount);

        Order savedOrder=orderRepository.save(order);

        List<OrderItemResponse> responseItems=savedOrder.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getProduct().getId(),
                        item.getProduct().getSku(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getLineTotal()))
                .toList();

        return new OrderResponse(savedOrder.getId(),
                savedOrder.getOrderNumber(),
                savedOrder.getStatus(),
                savedOrder.getTotalAmount(),
                responseItems);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, Authentication authentication){
        User user=userRepository.findByEmail(authentication.getName().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "User not found"));

        Customer customer=customerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Customer account not found"));

        Order order=orderRepository.findByIdAndCustomer_Id(orderId, customer.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Order not found"));

        List<OrderItemResponse> responseItems=order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getProduct().getId(),
                        item.getProduct().getSku(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getLineTotal()))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getTotalAmount(),
                responseItems
        );
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId, Authentication authentication){

        User user=userRepository.findByEmail(authentication.getName().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "User not found"));

        Customer customer=customerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Customer account not found"));

        Order order=orderRepository.findByIdAndCustomerIdForUpdate(orderId, customer.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Order not found"));

        if(!"PLACED".equals(order.getStatus())){
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only placed order can be cancelled");
        }

        List<OrderItem> items=order.getItems();

        List<Long> productIds=items.stream()
                .map(item -> item.getProduct().getId())
                .distinct()
                .sorted()
                .toList();
        Map<Long, Inventory> inventories=new LinkedHashMap<>();

        for(Long productId:productIds){
            Inventory inventory=inventoryRepository.findByProductIdForUpdate(productId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "Inventory not found for product: "+productId));

            inventories.put(productId, inventory);
        }

        for(OrderItem item:items){
            Inventory inventory=inventories.get(item.getProduct().getId());

            int reserved=inventory.getQuantityReserved();

            if(reserved<item.getQuantity()){
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Reserved quantity is inconsistent for product: "+item.getProduct().getId());
            }

            inventory.setQuantityReserved(reserved-item.getQuantity());
        }

        order.setStatus("CANCELLED");

        List<OrderItemResponse> responseItems=items.stream()
                .map(item -> new OrderItemResponse(
                        item.getProduct().getId(),
                        item.getProduct().getSku(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getLineTotal()
                ))
                .toList();

        return new OrderResponse(order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getTotalAmount(),
                responseItems
        );
    }

    @Transactional
    public OrderResponse fulfillOrder(Long orderId){

        Order order=orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Order is not found"));

        if(!"PLACED".equals(order.getStatus())){
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only PLACED orders can be fulfilled");
        }

        List<OrderItem> items=order.getItems();

        List<Long> productIds=items.stream()
                .map(item -> item.getProduct().getId())
                .distinct()
                .sorted()
                .toList();

        Map<Long, Inventory> inventories=new LinkedHashMap<>();

        for(Long productId:productIds){
            Inventory inventory=inventoryRepository.findByProductIdForUpdate(productId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "Inventory not found for product: "+productId));

            inventories.put(productId, inventory);
        }

        for(OrderItem item:items){
            Inventory inventory=inventories.get(item.getProduct().getId());

            if(inventory.getQuantityOnHand()<item.getQuantity()
            || inventory.getQuantityReserved()<item.getQuantity()){
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Insufficient on-hand or reserved stock for product: "
                        + item.getProduct().getId());
            }
        }

        for(OrderItem item:items){
            Inventory inventory=inventories.get(item.getProduct().getId());

            inventory.setQuantityOnHand(inventory.getQuantityOnHand()-item.getQuantity());
            inventory.setQuantityReserved(inventory.getQuantityReserved()-item.getQuantity());
        }

        order.setStatus("FULFILLED");

        List<OrderItemResponse> responseItems=items.stream()
                .map(item -> new OrderItemResponse(
                        item.getProduct().getId(),
                        item.getProduct().getSku(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getLineTotal()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getTotalAmount(),
                responseItems
        );
    }


    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(Authentication authentication) {

        User user = userRepository.findByEmail(
                authentication.getName().toLowerCase()
        ).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "User not found"));

        Customer customer = customerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Customer account not found"));

        return orderRepository
                .findByCustomer_IdOrderByCreatedAtDesc(customer.getId())
                .stream()
                .map(order -> {
                    List<OrderItemResponse> items = order.getItems()
                            .stream()
                            .map(item -> new OrderItemResponse(
                                    item.getProduct().getId(),
                                    item.getProduct().getSku(),
                                    item.getProduct().getName(),
                                    item.getQuantity(),
                                    item.getUnitPrice(),
                                    item.getLineTotal()
                            ))
                            .toList();

                    return new OrderResponse(
                            order.getId(),
                            order.getOrderNumber(),
                            order.getStatus(),
                            order.getTotalAmount(),
                            items
                    );
                })
                .toList();
    }


    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrdersForStaff(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(order -> {
                    List<OrderItemResponse> items = order.getItems()
                            .stream()
                            .map(item -> new OrderItemResponse(
                                    item.getProduct().getId(),
                                    item.getProduct().getSku(),
                                    item.getProduct().getName(),
                                    item.getQuantity(),
                                    item.getUnitPrice(),
                                    item.getLineTotal()
                            ))
                            .toList();

                    return new OrderResponse(
                            order.getId(),
                            order.getOrderNumber(),
                            order.getStatus(),
                            order.getTotalAmount(),
                            items);
                });
    }

}
