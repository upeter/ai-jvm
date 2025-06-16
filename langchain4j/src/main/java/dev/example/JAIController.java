package dev.example;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class JAIController {

    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;
    private final SimpleChatAssistant assistant;
    private final StreamChatAssistant streamingAssistant;

    public JAIController(ChatModel chatModel, StreamingChatModel streamingChatModel, SimpleChatAssistant assistant, StreamChatAssistant streamingAssistant) {
        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;
        this.assistant = assistant;
        this.streamingAssistant = streamingAssistant;
    }

    @GetMapping("/jai/ask")
    public String simpleChat(@RequestParam("message") String message) {
        return chatModel.chat(message);
    }

    @GetMapping("/jai/stream")
    public Flux<String> streamChat(@RequestParam("message") String message) {
        return streamingAssistant.chat(message);
    }
}