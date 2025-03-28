package dev.example;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

@Configuration
public class JAiConfig {

    @Bean
    @Description("Order dish for customer") // function description
    public OrderService orderService() {
        return new OrderService();
    }

    @Bean
    @Description("Find matching dishes based on dish name or ingredients") // function description
    public MenuService menuService(VectorStore vectorStore) {
        return new MenuService(vectorStore);
    }


}
