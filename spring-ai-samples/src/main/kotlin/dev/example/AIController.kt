package dev.example

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.ai.document.Document
import org.springframework.ai.openai.OpenAiAudioSpeechModel
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.web.bind.annotation.*
import java.util.stream.Collectors

@RestController
internal class AIController(chatClientBuilder: ChatClient.Builder, val vectorStore: VectorStore, chatMemory: ChatMemory, val openAiAudioSpeechModel: OpenAiAudioSpeechModel) {


    val chatClient =  chatClientBuilder
                .defaultAdvisors( SimpleLoggerAdvisor(),
//                    PromptChatMemoryAdvisor(chatMemory) ,
                    MessageChatMemoryAdvisor(chatMemory)) // CHAT MEMORY
                    //QuestionAnswerAdvisor(vectorStore, SearchRequest.defaults())
                .build()

    @GetMapping("/ai") fun completion(
        @RequestParam(value = "message", defaultValue = "Tell me a joke") message: String,
    ): Map<String, String> = mapOf("completion" to
            chatClient.prompt()
                .user(message)
                .call()
                .content()
        )

    @PostMapping("/ai")
    fun completion(@RequestBody chatRequest: ChatRequest): Map<String, String> {
        val response = chatClient.prompt()
            .user(chatRequest.message)
            .run { chatRequest.system?.let{system(chatRequest.system)} ?: this}
            .call()
            .content()
        return mapOf("completion" to response)
    }

    @PostMapping("/ai/stream")
    fun streamCompletion(@RequestBody chatRequest: ChatRequest): Flow<String> {
        val response = chatClient.prompt()
            .user(chatRequest.message)
            .run { chatRequest.system?.let{system(chatRequest.system)} ?: this}
            .stream()
            .content()

        return response.asFlow()
    }

    @PostMapping("/ai/weather")
    fun weather(@RequestBody chatInput: ChatInput): String {
        return this.chatClient
            .prompt()
            .user(chatInput.message)
            //.advisors{it.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatInput.conversationId).param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 200)}
            .functions("weatherService")
//            .advisors(Consumer<ChatClient.AdvisorSpec> { a: ChatClient.AdvisorSpec ->
//                a.param(CustomChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, chatInput.conversationId())
//                a.param(CustomChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10)
//            })
            .call()
            .content()
    }


    @PostMapping("/ai/chat")
    fun chat(@RequestBody chatInput: ChatInput): String {
        val relatedDocuments: List<Document> = vectorStore.similaritySearch(chatInput.message)
        return this.chatClient
            .prompt()
            .system(SYSTEM_PROMPT)
            .user(createPrompt(chatInput.message, relatedDocuments, chatInput.latitude.toString(), chatInput.longitude.toString()))
            .advisors{it.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatInput.conversationId).param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 200)}
            .functions("foodOrderService")
//            .advisors(Consumer<ChatClient.AdvisorSpec> { a: ChatClient.AdvisorSpec ->
//                a.param(CustomChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, chatInput.conversationId())
//                a.param(CustomChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10)
//            })
            .call()
            .content()
    }

    @PostMapping("/ai/speech")
    fun speech(@RequestBody chatInput: ChatInput): ByteArray {
        val text = chat(chatInput)
        return openAiAudioSpeechModel.call(text)

    }

    private fun createPrompt(query: String, context: List<Document>, latitude: String, longitude: String): String {
        val promptTemplate = PromptTemplate(ORDER_PROMPT)
        promptTemplate.add("query", query)
        promptTemplate.add("latitude", latitude)
        promptTemplate.add("longitude", longitude)
        promptTemplate.add("context", context.map { it.content }.joinToString(prefix = "- ", separator = "\n"))
        return promptTemplate.render()
    }
//, yet be crisp, because you responses will be converted to speech.
    companion object {
        val SYSTEM_PROMPT = """
           "You are an Italian Waiter. Respond in a friendly, helpful, crisp and joyful manner.

           You have to find the best matching meal based on food preference for a cat.

           At the very beginning you start with: 'Hey cat, you look hungry. Can I order a delicious meal for you? Tell me what you want'. In subsequent conversation don't use this phrase anymore. 
          
           The cat has to supply food preferences, like Ravioli, Spaghetti etc., or ingredients like Flour, Cream etc.
           
           Based on the food preferences, RAG will lookup dishes, which it adds to the user context. The cat has to chose one of the proposed dishes or change its food preferences. Only propose dishes from the context, don't invent dishes yourself. 
           
           Before ordering, the cat must agree with one dish.
            
           Once the dish is chosen summarize the order without mentioning ingredients and close the conversation with: I will order these for you, stay put! After this phrase, trigger the `foodOrderService` function to place the order. 
           
     
           """
//           In the very beginning you start with: 'Hey cat, you look hungry. Can I order a delicious meal for you?'
//

        val ORDER_PROMPT = """
            For dishes only use the following context, nothing else. If the conversation does not involve meal preferences and is not related to food, skip the context:
            {context}
            
            foodOrderService context:
            - Latitude: {latitude}
            - Longitude: {longitude}
            
            Query:
            {query}
            """

    }

}


data class ChatRequest(val message: String, val system: String? = null)
data class ChatInput(val message: String, val conversationId: String, val latitude:String = "40.730610",  val longitude:String = "-73.935242")

