package com.rapipedido.api.orders;

import com.rapipedido.api.catalog.Dish;
import com.rapipedido.api.catalog.DishRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.rapipedido.api.notification.OrderStatusChangedEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final DishRepository dishRepository;
    private final ApplicationEventPublisher events;

    public OrderService(OrderRepository orderRepository, DishRepository dishRepository,
                        ApplicationEventPublisher events) {
        this.orderRepository = orderRepository;
        this.dishRepository = dishRepository;
        this.events = events;
    }

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        validateRequest(request);

        BigDecimal total = BigDecimal.ZERO;
        java.util.List<OrderItem> validatedItems = new java.util.ArrayList<>();

        for (OrderItemRequest itemRequest : request.getItems()) {
            validateItem(itemRequest);
            Dish dish = dishRepository.findById(itemRequest.getDishId())
                    .orElseThrow(() -> new OrderValidationException("DISH_NOT_FOUND",
                            "No existe el plato con identificador " + itemRequest.getDishId()));

            if (!dish.isAvailable()) {
                throw new OrderValidationException("DISH_NOT_AVAILABLE",
                        "El plato " + dish.getName() + " no está disponible");
            }
            if (!dish.getRestaurantId().equals(request.getRestaurantId())) {
                throw new OrderValidationException("DISH_RESTAURANT_MISMATCH",
                        "El plato " + dish.getName() + " no pertenece al restaurante indicado");
            }

            BigDecimal catalogPrice = BigDecimal.valueOf(dish.getPrice()).setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal sentPrice = BigDecimal.valueOf(itemRequest.getUnitPrice()).setScale(2, java.math.RoundingMode.HALF_UP);
            if (catalogPrice.compareTo(sentPrice) != 0) {
                throw new OrderValidationException("INVALID_UNIT_PRICE",
                        "El precio enviado para " + dish.getName() + " no coincide con el precio del catálogo");
            }

            BigDecimal subtotal = catalogPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            total = total.add(subtotal);
            validatedItems.add(new OrderItem(dish.getId(), dish.getName(), itemRequest.getQuantity(), catalogPrice, subtotal));
        }

        Order order = new Order("ord_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12),
                request.getClientId().trim(), request.getRestaurantId(), request.getDeliveryAddress().trim(),
                request.getPaymentMethod().trim(), total, "CREATED", Instant.now());
        validatedItems.forEach(order::addItem);
        Order saved = orderRepository.save(order);
        events.publishEvent(new OrderStatusChangedEvent(
                saved.getId(), request.getClientId().trim(), "CREATED"));
        return new OrderResponse(saved.getId(), saved.getTotalAmount(), saved.getOrderStatus(), saved.getCreatedAt());
    }

    private void validateRequest(OrderRequest request) {
        if (request == null) throw new OrderValidationException("INVALID_REQUEST", "El cuerpo del pedido es obligatorio");
        if (request.getClientId() == null || request.getClientId().isBlank())
            throw new OrderValidationException("INVALID_CLIENT_ID", "El clientId es obligatorio");
        if (request.getRestaurantId() == null || request.getRestaurantId() <= 0)
            throw new OrderValidationException("INVALID_RESTAURANT_ID", "El restaurantId debe ser mayor que cero");
        if (request.getDeliveryAddress() == null || request.getDeliveryAddress().isBlank())
            throw new OrderValidationException("INVALID_DELIVERY_ADDRESS", "La dirección de entrega es obligatoria");
        if (request.getPaymentMethod() == null || request.getPaymentMethod().isBlank())
            throw new OrderValidationException("INVALID_PAYMENT_METHOD", "El método de pago es obligatorio");
        if (request.getItems() == null || request.getItems().isEmpty())
            throw new OrderValidationException("EMPTY_ORDER", "El pedido debe contener al menos un producto");
    }

    private void validateItem(OrderItemRequest item) {
        if (item == null || item.getDishId() == null || item.getDishId() <= 0)
            throw new OrderValidationException("INVALID_DISH_ID", "El identificador del plato debe ser mayor que cero");
        if (item.getQuantity() == null || item.getQuantity() <= 0)
            throw new OrderValidationException("INVALID_QUANTITY", "La cantidad de cada producto debe ser mayor que cero");
        if (item.getUnitPrice() == null || !Double.isFinite(item.getUnitPrice()) || item.getUnitPrice() < 0)
            throw new OrderValidationException("INVALID_UNIT_PRICE", "El precio unitario debe ser mayor o igual que cero");
    }

    @Transactional
    public OrderResponse updateStatus(String orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderValidationException("ORDER_NOT_FOUND",
                        "No existe el pedido " + orderId));
        if (newStatus == null || !java.util.Set.of("READY", "DELIVERED").contains(newStatus.toUpperCase())) {
            throw new OrderValidationException("INVALID_STATUS", "Estado no permitido: use READY o DELIVERED");
        }
        order.setOrderStatus(newStatus.toUpperCase());
        Order saved = orderRepository.save(order);
        events.publishEvent(new OrderStatusChangedEvent(
                saved.getId(), saved.getClientId(), saved.getOrderStatus()));
        return new OrderResponse(saved.getId(), saved.getTotalAmount(),
                saved.getOrderStatus(), saved.getCreatedAt());
    }
}
