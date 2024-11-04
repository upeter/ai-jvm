package dev.example;

import dev.example.demo.Assistant;
import dev.example.demo.StreamingAssistant;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class JAIController {

    private final ChatLanguageModel chatLanguageModel;
    private final StreamingChatLanguageModel streamingChatModel;
    private final SimpleChatAssistant assistant;
    private final StreamChatAssistant streamingAssistant;

    public JAIController(ChatLanguageModel chatLanguageModel, StreamingChatLanguageModel streamingChatModel, SimpleChatAssistant assistant, StreamChatAssistant streamingAssistant) {
        this.chatLanguageModel = chatLanguageModel;
        this.streamingChatModel = streamingChatModel;
        this.assistant = assistant;
        this.streamingAssistant = streamingAssistant;
    }

    @GetMapping("/ai/ask")
    public String simpleChat(@RequestParam("message") String message) {
        return chatLanguageModel.generate(message);
    }

    @GetMapping("/ai/stream")
    public Flux<String> streamChat(@RequestParam("message") String message) {
        return streamingAssistant.chat(message);
    }
}