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
public class SpringAISampleController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final OpenAiAudioSpeechModel openAiAudioSpeechModel;

    public SpringAISampleController(ChatClient.Builder chatClientBuilder, VectorStore vectorStore, ChatMemory chatMemory, OpenAiAudioSpeechModel openAiAudioSpeechModel) {
        this.chatClient = chatClientBuilder.defaultAdvisors(new SimpleLoggerAdvisor()).build();//, new MessageChatMemoryAdvisor(chatMemory)).build();
        this.vectorStore = vectorStore;
        this.openAiAudioSpeechModel = openAiAudioSpeechModel;
    }






















    private static String createPrompt(String query, List<Document> context) {
        PromptTemplate promptTemplate = new PromptTemplate(USER_PROMPT);
        promptTemplate.add("query", query);
        promptTemplate.add("context", context.stream()
                .map(doc -> "Dish: " + doc.getMetadata().get("Name") + " Dish with Ingredients: " + doc.getContent())
                .collect(Collectors.joining("\n - ", "- ", "")));
        return promptTemplate.render();
    }


    private static final String SYSTEM_PROMPT = """
             You are an Italian waiter. Respond in a friendly, helpful yet crisp manner always in English.
            
             Objective: Assist the customer in choosing and ordering the best matching meal based on given food preferences.
            
             Food Preferences: The customer will provide food preferences, such as specific dishes like Ravioli or Spaghetti, or ingredients like Cheese or Cream.
            
             Dish Suggestions:
             Use the context provided in the user message under 'Dish Context'.
             Only propose dishes from this context; do not invent dishes yourself. Propose all the possible options from the context.
             Assist the customer in choosing one of the proposed dishes or encourage him/her to adjust their food preferences if needed.
            """;

    private static final String USER_PROMPT = """
            User Query:
            {query}
            
            For dishes use the following context:
            {context}""";



    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChatInput(String message, String conversationId) {
        public ChatInput(String message) {
            this(message, UUID.randomUUID().toString());
        }
    }




}

