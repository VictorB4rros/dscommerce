package com.devsuperior.dscommerce.tests;

import com.devsuperior.dscommerce.dto.OrderDTO;
import com.devsuperior.dscommerce.entities.*;

import java.time.Instant;

public class OrderFactory {

    public static Order createOrder(User client) {
        Order order = new Order(1L, Instant.now(), OrderStatus.WAITING_PAYMENT, client, new Payment());
        Product product = ProductFactory.createProduct();
        OrderItem orderItem = new OrderItem(order, product, 2, 10.0);
        order.getItems().add(orderItem);
        return order;
    }

    public static Order createOrderToInsert(User client) {
        Order order = new Order(null, Instant.now(), OrderStatus.WAITING_PAYMENT, client, new Payment());
        Product product = ProductFactory.createProduct();
        OrderItem orderItem = new OrderItem(order, product, 2, 10.0);
        order.getItems().add(orderItem);
        return order;
    }

    public static OrderDTO createOrderDTO(User client) {
        Order order = createOrderToInsert(client);
        return new OrderDTO(order);
    }

    public static OrderDTO createInvalidOrderDTO(User client) {
        Order order = createOrderToInsert(client);
        order.getItems().clear();
        return new OrderDTO(order);
    }
}
