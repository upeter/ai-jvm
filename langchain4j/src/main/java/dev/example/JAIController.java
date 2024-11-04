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
    private final StreamChatAssistant streamingAssistant;

    public JAIController(ChatLanguageModel chatLanguageModel, StreamChatAssistant streamingAssistant) {
        this.chatLanguageModel = chatLanguageModel;
        this.streamingAssistant = streamingAssistant;
    }

}