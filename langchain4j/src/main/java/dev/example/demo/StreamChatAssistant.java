package dev.example.demo;

import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

@AiService
public interface StreamChatAssistant {

    Flux<String> chat(String userMessage);
}

