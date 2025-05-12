package dev.example

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.modelcontextprotocol.client.McpSyncClient
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.ai.converter.BeanOutputConverter
import org.springframework.ai.converter.StructuredOutputConverter
import org.springframework.ai.document.Document
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider
import org.springframework.ai.openai.OpenAiAudioSpeechModel
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel
import org.springframework.ai.openai.audio.speech.SpeechPrompt
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.core.ParameterizedTypeReference
import org.springframework.core.io.InputStreamResource
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import reactor.core.publisher.Flux
import java.util.*

@RestController
internal class AIController(
    val vectorStore: VectorStore,
    val openAiAudioSpeechModel: OpenAiAudioSpeechModel,
    val openAiAudioTranscriptionModel: OpenAiAudioTranscriptionModel,
    val mcpSyncClients: List<McpSyncClient>,
    val chatClient: ChatClient,
) {

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
    
            Order:
            When the client has made a choice trigger the 'orderService' function.
    
            Once the function is successfully called, close the conversation with: "Thank you for your order"
    
            Then summarize the ordered dishes without mentioning the ingredients and give a
            time indication in minutes as returned by the 'orderService' function.
        """

        val USER_PROMPT = """       
           User Query:
           {query}

           Dish context:
           {context}
           """

        private fun createPrompt(query: String, context: List<Document>): String {
            val promptTemplate = PromptTemplate(USER_PROMPT)
            promptTemplate.add("query", query)
            promptTemplate.add("context", context.map { "Dish: ${it.metadata["Name"] } Dish with Ingredients: ${it.text}" }.joinToString(prefix = "- ", separator = "\n - "))
            return promptTemplate.render()
        }


        val MCP_PROMPT = """ 
            You are an Italian waiter AI who assists customers in choosing and ordering dishes.
            Here's how to behave:
            - If the user wants to have some ideas about dishes run `complete-menu-italian-delaight-restaurant` to have the complete menu.
            - If the user gives food preferences or ingredients, use the `find-dishes-service` tool to find matching dishes.
            - before looking for preferred meals first run the `classify-prompt-if-food-or-other` tool to understand whether the prompt is about a food preference or not.
            - Propose ALL matching dishes from the `find-dishes-service` result.
            - If the customer confirms a dish, call the `order-dish-service` tool.
            - Once the order is placed, thank them and summarize the dish names with the estimated delivery time.
            
            Only use the tools to gather or act on information. Do not invent dishes. Be polite, helpful, and speak in a friendly English tone.
            """.trimIndent()



        inline fun <reified T> ChatClient.CallResponseSpec.entity(): T? =
            entity(object: StructuredOutputConverter<T> by BeanOutputConverter(object:ParameterizedTypeReference<T>(){}, jacksonObjectMapper()) {})
    }

}


data class ChatInput(val message: String, val conversationId: String = UUID.randomUUID().toString())

enum class Classification {
    FOOD, OTHER
}
data class PromptClassification(val classification: Classification, val foodElements: List<String>)


@JsonIgnoreProperties(ignoreUnknown = true)
data class Dishes(val dishes:List<Dish>)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Dish (val dish: String, val ingredients: List<String>)
