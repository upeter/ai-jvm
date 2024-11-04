package dev.example;

import dev.langchain4j.service.spring.AiService;

@AiService
public interface SimpleChatAssistant {
    String chat(String userMessage);
}
