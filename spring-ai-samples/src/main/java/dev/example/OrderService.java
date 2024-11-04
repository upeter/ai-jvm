package dev.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.function.Function;

public class OrderService implements Function<OrderRequest, OrderResponse> {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Override
    public OrderResponse apply(OrderRequest orderRequest) {
        logger.info(
                "\n*****************************************************************************\n" +
                        "🍕🍕🍕 Ordering dishes: " + String.join("\n- ", orderRequest.meals()) + " 🍕🍕🍕\n" +
                        "*****************************************************************************\n\n");
        return new OrderResponse(20);
    }
}
