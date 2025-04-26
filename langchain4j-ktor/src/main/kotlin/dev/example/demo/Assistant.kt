package dev.example.demo

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import dev.langchain4j.service.SystemMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import reactor.core.publisher.Flux

/**
 * Assistant interface for non-streaming chat
 */
class Assistant(private val chatLanguageModel: ChatLanguageModel) {
    
    /**
     * Chat with the assistant
     * @param userMessage The user's message
     * @return The assistant's response
     */
    fun chat(userMessage: String): String {
        // In Spring Boot, this is handled by the @AiService annotation
        // Here we need to manually create the prompt with system message
        val systemMessage = "You are a polite assistant"
        val prompt = dev.langchain4j.data.message.SystemMessage(systemMessage)
        val userMessageObj = dev.langchain4j.data.message.UserMessage(userMessage)
        
        return chatLanguageModel.generate(listOf(prompt, userMessageObj))
            .content()
            .text()
    }
}

/**
 * Assistant interface for streaming chat
 */
class StreamingAssistant(private val streamingChatLanguageModel: StreamingChatLanguageModel) {
    
    /**
     * Chat with the assistant in streaming mode
     * @param userMessage The user's message
     * @return A flow of response chunks
     */
    fun chat(userMessage: String): Flow<String> {
        // In Spring Boot, this is handled by the @AiService annotation
        // Here we need to manually create the prompt with system message
        val systemMessage = "You are a polite assistant"
        val prompt = dev.langchain4j.data.message.SystemMessage(systemMessage)
        val userMessageObj = dev.langchain4j.data.message.UserMessage(userMessage)
        
        val flux = Flux.from(streamingChatLanguageModel.generate(listOf(prompt, userMessageObj)))
            .map { it.content().text() }
        
        return flux.asFlow()
    }
}