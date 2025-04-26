package dev.example.demo

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.openai.OpenAiStreamingChatModel
import io.ktor.server.config.*
import java.time.Duration

fun configureChatLanguageModel(config: ApplicationConfig): ChatLanguageModel {
    val apiKey = config.property("api-key").getString()
    val modelName = config.property("model-name").getString()
    val temperature = config.property("temperature").getString().toDouble()
    val timeout = Duration.parse(config.property("timeout").getString())
    val logRequests = config.property("log-requests").getString().toBoolean()
    val logResponses = config.property("log-responses").getString().toBoolean()
    
    return OpenAiChatModel.builder()
        .apiKey(apiKey)
        .modelName(modelName)
        .temperature(temperature)
        .timeout(timeout)
        .logRequests(logRequests)
        .logResponses(logResponses)
        .build()
}

fun configureStreamingChatLanguageModel(config: ApplicationConfig): StreamingChatLanguageModel {
    val apiKey = config.property("api-key").getString()
    val modelName = config.property("model-name").getString()
    
    return OpenAiStreamingChatModel.builder()
        .apiKey(apiKey)
        .modelName(modelName)
        .build()
}