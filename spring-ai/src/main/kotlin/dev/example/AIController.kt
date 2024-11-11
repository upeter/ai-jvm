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
    @Value("classpath:italian-food.png") val image:Resource,
    chatClientBuilder: ChatClient.Builder, val vectorStore: VectorStore, chatMemory: ChatMemory, val openAiAudioSpeechModel: OpenAiAudioSpeechModel) {

    private val chatClient = chatClientBuilder.defaultAdvisors( SimpleLoggerAdvisor(), MessageChatMemoryAdvisor(chatMemory)).build()

    @GetMapping("/kai/stream")
    fun simplePrompt(@RequestParam("message") message: String): Flux<String> =
        chatClient.prompt()
            .user(message)
            .stream()
            .content()


    @GetMapping("/kai/top-dishes-per-kitchen")
    fun simplePromptWithConversion(@RequestParam("kitchen") kitchen: String): Dishes =
        chatClient.prompt()
            .user{
                it.text("Select the most wanted dishes for the following kitchen: {kitchen} with the main ingredients")
                .param("kitchen", kitchen)}
            .call()
            .entity<Dishes>()



    @GetMapping("/kai/media-prompt")
    fun mediaPrompt(@RequestParam("url") url: URL): Flux<String> =
        chatClient.prompt()
            .user{it.text("Detect all the objects in the image")
                .media(MimeTypeUtils.IMAGE_JPEG, url)
            }
            .stream()
            .content()




    @PostMapping("/kai/chat")
    fun chat(@RequestBody chatInput: ChatInput): String {
        val relatedDocuments: List<Document> = vectorStore.similaritySearch(chatInput.message)
        return this.chatClient
            .prompt()
            .system(SYSTEM_PROMPT)
            .user(createPrompt(chatInput.message, relatedDocuments))
            .advisors{it.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatInput.conversationId)
                .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 50)}
            .call()
            .content()
    }



    @PostMapping("/kai/speech")
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
       
           """


        val USER_PROMPT = """       
           User Query:
           {query}
            
           For dishes use the following context:
           {context}"""

        private fun createPrompt(query: String, context: List<Document>): String {
            val promptTemplate = PromptTemplate(USER_PROMPT)
            promptTemplate.add("query", query)
            promptTemplate.add("context", context.map { "Dish: ${it.metadata["Name"] } Dish with Ingredients: ${it.content}" }.joinToString(prefix = "- ", separator = "\n - "))
            return promptTemplate.render()
        }

        inline fun <reified T> ChatClient.CallResponseSpec.entity(): T =
            entity(object: StructuredOutputConverter<T> by BeanOutputConverter(object:ParameterizedTypeReference<T>(){}, jacksonObjectMapper()) {})
    }

}


data class ChatInput(val message: String, val conversationId: String = UUID.randomUUID().toString())

@JsonIgnoreProperties(ignoreUnknown = true)
data class Dishes(val dishes:List<Dish>)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Dish (val dish: String, val ingredients: List<String>)

