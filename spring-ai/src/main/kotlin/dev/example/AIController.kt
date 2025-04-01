package dev.example

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.ai.converter.BeanOutputConverter
import org.springframework.ai.converter.StructuredOutputConverter
import org.springframework.ai.document.Document
import org.springframework.ai.openai.OpenAiAudioSpeechModel
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.core.io.Resource
import org.springframework.util.MimeType
import org.springframework.util.MimeTypeUtils
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import java.net.URL
import java.util.UUID

@RestController
internal class AIController(
    chatClientBuilder: ChatClient.Builder, val vectorStore: VectorStore, chatMemory: ChatMemory, val openAiAudioSpeechModel: OpenAiAudioSpeechModel) {

    private val chatClient = chatClientBuilder.defaultAdvisors( SimpleLoggerAdvisor(), MessageChatMemoryAdvisor(chatMemory)).build()

    @GetMapping("/ai/stream")
    fun simplePrompt(@RequestParam("message") message: String): Flux<String> =
        chatClient.prompt()
            .user(message)
            .stream()
            .content()


























    companion object {
        const val SYSTEM_PROMPT = """
        You are an Italian waiter. Respond in a friendly, helpful manner always in English.

        Objective: Assist the customer in choosing and ordering the best matching meal based on given food preferences.

        Initial Greeting: Always start the initial conversation with: 'Welcome to Italian DelAIght! How can I help you today?'. Don't use this phrase later in the conversation. 

        Food Preferences: The customer  will provide food preferences, such as specific dishes like Ravioli or Spaghetti, or ingredients like Cheese or Cream.

        Dish Suggestions:
        Only if the user input is about food preferences use the context provided in the user message under 'Dish Context'.
        Only propose dishes from this context; do not invent dishes yourself. Propose ALL the possible options from the context.
        Assist the customer in choosing one of the proposed dishes or encourage him/her to adjust their food preferences if needed.

        """


        val USER_PROMPT = """       
           User Query:
           {query}

        Dish context:
        {context}"""

        private fun createPrompt(query: String, context: List<Document>): String {
            val promptTemplate = PromptTemplate(USER_PROMPT)
            promptTemplate.add("query", query)
            promptTemplate.add("context", context.map { "Dish: ${it.metadata["Name"] } Dish with Ingredients: ${it.text}" }.joinToString(prefix = "- ", separator = "\n - "))
            return promptTemplate.render()
        }

        inline fun <reified T> ChatClient.CallResponseSpec.entity(): T? =
            entity(object: StructuredOutputConverter<T> by BeanOutputConverter(object:ParameterizedTypeReference<T>(){}, jacksonObjectMapper()) {})
    }

}


data class ChatInput(val message: String, val conversationId: String = UUID.randomUUID().toString())


