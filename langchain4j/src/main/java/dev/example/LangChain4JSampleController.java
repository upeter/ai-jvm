package dev.example;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class LangChain4JSampleController {

    private final ChatLanguageModel chatLanguageModel;
    private final StreamChatAssistant streamingAssistant;

    public LangChain4JSampleController(ChatLanguageModel chatLanguageModel, StreamChatAssistant streamingAssistant) {
        this.chatLanguageModel = chatLanguageModel;
        this.streamingAssistant = streamingAssistant;
    }




}