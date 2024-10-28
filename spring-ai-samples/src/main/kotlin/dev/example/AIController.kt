package dev.example

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
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
import org.springframework.core.ParameterizedTypeReference
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
internal class AIController(chatClientBuilder: ChatClient.Builder, val vectorStore: VectorStore, chatMemory: ChatMemory, val openAiAudioSpeechModel: OpenAiAudioSpeechModel) {

    private val chatClient = chatClientBuilder.defaultAdvisors( SimpleLoggerAdvisor(), MessageChatMemoryAdvisor(chatMemory)).build()

    @GetMapping("/ai/top-dishes-per-kitchen")
    fun stream(@RequestParam("kitchen") kitchen: String): Dishes =
        chatClient.prompt()
            .user{it.text("Select the most wanted dishes for the following kitchen: {kitchen} with the main ingredients").param("kitchen", kitchen)}
            .call()
            .entity<Dishes>()


    @PostMapping("/ai/stream")
    fun streamCompletion(@RequestBody chatInput: ChatInput): Flow<String> =
        chatClient.prompt()
            .user(chatInput.message)
            .stream()
            .content()
            .asFlow()


    @PostMapping("/ai/chat")
    fun chat(@RequestBody chatInput: ChatInput): String {
        val relatedDocuments: List<Document> = vectorStore.similaritySearch(chatInput.message)
        return this.chatClient
            .prompt()
            .system(SYSTEM_PROMPT)
            .user(createPrompt(chatInput.message, relatedDocuments))
            .advisors{it.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatInput.conversationId).param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 200)}
            .functions("orderService")
            .call()
            .content()
    }

    @PostMapping("/ai/speech")
    fun speech(@RequestBody chatInput: ChatInput): ByteArray {
        val text = chat(chatInput)
        return openAiAudioSpeechModel.call(text)
    }





    companion object {
        val SYSTEM_PROMPT = """
            You are an Italian waiter. Respond in a friendly, helpful yet crisp manner always in English.

            Objective: Assist the customer in choosing and ordering the best matching meal based on given food preferences.
            
            Food Preferences: The customer will provide food preferences, such as specific dishes like Ravioli or Spaghetti, or ingredients like Cheese or Cream.

            Dish Suggestions:
            Use the context provided in the user message under 'Dish Context'.
            Only propose dishes from this context; do not invent dishes yourself. Propose all the possible options from the context.
            Assist the customer in choosing one of the proposed dishes or encourage him/her to adjust their food preferences if needed.
        
            Post-Order Actions:          
            After confirming the order, trigger the 'orderService' function.
            Once the function is successfully called, close the conversation with: "Thank you for your order" 
            Then summarize the ordered meals and give a time indication in minutes as returned by the 'orderService' function.
           """


        val USER_PROMPT = """       
           User Query:
           {query}
            
            For dishes only use the following context.

           {context}"""

        private fun createPrompt(query: String, context: List<Document>): String {
            val promptTemplate = PromptTemplate(USER_PROMPT)
            promptTemplate.add("query", query)
            promptTemplate.add("context", context.map { "Dish: ${it.metadata["Name"] } Dish with Ingredients: ${it.content}" }.joinToString(prefix = "- ", separator = "\n - "))
            return promptTemplate.render()
        }

        val mapper = jacksonObjectMapper()
        inline fun <reified T> ChatClient.CallResponseSpec.entity(): T = entity(object: StructuredOutputConverter<T> {
            val converter = BeanOutputConverter(object:ParameterizedTypeReference<T>(){}, mapper)
            override fun convert(source: String): T?  = converter.convert(source)
            override fun getFormat(): String = converter.format

        })
    }

}


data class ChatInput(val message: String, val conversationId: String = UUID.randomUUID().toString())

@JsonIgnoreProperties(ignoreUnknown = true)
data class Dishes(val dishes:List<Dish>)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Dish (val dish: String, val ingredients: List<String>)

//Use the dish context provided in the user message under 'Dish Context'.

