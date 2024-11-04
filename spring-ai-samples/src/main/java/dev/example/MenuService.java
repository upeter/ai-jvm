package dev.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MenuService implements Function<MenuRequest, MenuResponse> {
    private static final Logger logger = LoggerFactory.getLogger(MenuService.class);
    private final VectorStore vectorStore;

    public MenuService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public MenuResponse apply(MenuRequest dish) {
        List<String> dishes = vectorStore.similaritySearch(dish.dish()).stream()
                .map(doc -> "Dish: " + doc.getMetadata().get("Name") + " Dish with Ingredients: " + doc.getContent())
                .toList();
        logger.info(
                "\n-------------------------------------------------------------\n" +
                        "🧑‍🍳Calling menu service 🧑‍🍳\n" + dishes.stream().collect(Collectors.joining("\n - ", "- ", "")) +
                        "-------------------------------------------------------------\n\n");
        return new MenuResponse(dishes);
    }
}