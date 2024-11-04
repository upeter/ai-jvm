package dev.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.example.utils.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.net.URL;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
public class JAIController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final OpenAiAudioSpeechModel openAiAudioSpeechModel;

    public JAIController(ChatClient.Builder chatClientBuilder, VectorStore vectorStore, ChatMemory chatMemory, OpenAiAudioSpeechModel openAiAudioSpeechModel) {
        this.chatClient = chatClientBuilder.defaultAdvisors(new SimpleLoggerAdvisor(), new MessageChatMemoryAdvisor(chatMemory)).build();
        this.vectorStore = vectorStore;
        this.openAiAudioSpeechModel = openAiAudioSpeechModel;
    }


    @GetMapping("/ai/stream")
    public Flux<String> simplePrompt(@RequestParam("message") String message) {
        return chatClient.prompt()
                .user(message)
                .stream()
                .content();
    }



    @GetMapping("/ai/top-dishes-per-kitchen")
    public Dishes simplePromptWithConversion(@RequestParam("kitchen") String kitchen) {
        return chatClient.prompt()
                .user(template -> template.text("Select the most wanted dishes for the following kitchen: {kitchen} with the main ingredients")
                        .param("kitchen", kitchen))
                .call()
                .entity(Dishes.class);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Dish(String dish, List<String> ingredients) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Dishes(List<Dish> dishes) {}



    @GetMapping("/ai/media-prompt")
    public Flux<String> mediaPrompt(@RequestParam("url") URL url) {
        return chatClient.prompt()
                .user(it -> it.text("Detect all the objects in the image")
                        .media(MimeTypeUtils.IMAGE_JPEG, url))
                .stream()
                .content();
    }

    @PostMapping("/ai/chat")
    public String chat(@RequestBody ChatInput chatInput) {
//        List<Document> relatedDocuments = vectorStore.similaritySearch(chatInput.message());
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(createPrompt(chatInput.message()))
                .advisors(it -> it.param(AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, chatInput.conversationId())
                        .param(AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY, 50))
                .functions("menuService", "orderService")
                .call()
                .content();
    }

    @PostMapping("/ai/speech")
    public byte[] speech(@RequestBody ChatInput chatInput) {
        String text = chat(chatInput);
        return openAiAudioSpeechModel.call(text);
    }



    private static String createPrompt(String query, List<Document> context) {
        PromptTemplate promptTemplate = new PromptTemplate(USER_PROMPT);
        promptTemplate.add("query", query);
        promptTemplate.add("context", context.stream()
                .map(doc -> "Dish: " + doc.getMetadata().get("Name") + " Dish with Ingredients: " + doc.getContent())
                .collect(Collectors.joining("\n - ", "- ", "")));
        return promptTemplate.render();
    }


    private static String createPrompt(String query) {
        PromptTemplate promptTemplate = new PromptTemplate(USER_PROMPT);
        promptTemplate.add("query", query);
        return promptTemplate.render();
    }




    private static final String SYSTEM_PROMPT_ = """
             You are an Italian waiter. Respond in a friendly, helpful yet crisp manner always in English.
            
             Objective: Assist the customer in choosing and ordering the best matching meal based on given food preferences.
            
             Food Preferences: The customer will provide food preferences, such as specific dishes like Ravioli or Spaghetti, or ingredients like Cheese or Cream.
            
             Dish Suggestions:
             Use the context provided in the user message under 'Dish Context'.
             Only propose dishes from this context; do not invent dishes yourself. Propose all the possible options from the context.
             Assist the customer in choosing one of the proposed dishes or encourage him/her to adjust their food preferences if needed.
            """;

    private static final String USER_PROMPT_ = """
            User Query:
            {query}
            
            For dishes use the following context:
            {context}""";


    private static final String SYSTEM_PROMPT = """
            You are an Italian waiter. Respond in a friendly, helpful manner always in English.

            Objective: Assist the customer in choosing and ordering the best matching meal based on given food preferences.
            
            Food Preferences: The customer  will provide food preferences, such as specific dishes like Ravioli or Spaghetti, or ingredients like Cheese or Cream.
            
            Dish Suggestions:         
            Classify Input:         
            Determine whether the prompt represents a dish, food, or ingredient preference (Yes or No).
            
            If Yes (New Preferences):       
            Call the 'menuService' with the original prompt stripped of all non-food-related content.
            Use only the reply from the 'menuService' to propose dishes; do not invent dishes yourself.
            Assist the customer in choosing one of the proposed dishes or encourage them to adjust their food preferences if needed.
            Important: If the customer is confirming or choosing one of the previously proposed dishes, do not call the 'menuService' again, even if their prompt includes dish names or ingredients.
            
            If No:            
            Politely ask the customer to specify their food preferences or suggest some ingredients or dishes they like.
                       
            Order Confirmation:          
            Acknowledgment:           
            If the customer intends to order one of the proposed dishes, proceed without calling the 'menuService' again.
            
            Summarize Order:          
            Summarize the order without mentioning the ingredients.
           
            Post-Order Actions:          
            After confirming the order, trigger the 'orderService' function.
            Once the function is successfully called, close the conversation with: "Thank you for your order" 
            Then summarize the ordered meals and give a time indication in minutes as returned by the 'orderService' function.
           """;

    private static final String USER_PROMPT = """
          
            User Query:
            {query}
            """;




    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChatInput(String message, String conversationId) {
        public ChatInput(String message) {
            this(message, UUID.randomUUID().toString());
        }
    }




}

