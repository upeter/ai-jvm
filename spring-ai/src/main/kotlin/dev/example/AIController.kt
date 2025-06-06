package dev.example

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.modelcontextprotocol.client.McpSyncClient
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.ai.converter.BeanOutputConverter
import org.springframework.ai.converter.StructuredOutputConverter
import org.springframework.ai.document.Document
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider
import org.springframework.ai.openai.OpenAiAudioSpeechModel
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel
import org.springframework.ai.openai.audio.speech.SpeechPrompt
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.core.ParameterizedTypeReference
import org.springframework.core.io.InputStreamResource
import org.springframework.util.MimeTypeUtils
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import reactor.core.publisher.Flux
import java.net.URL
import java.util.*

@RestController
internal class AIController(
    val vectorStore: VectorStore,
    val openAiAudioSpeechModel: OpenAiAudioSpeechModel,
    val openAiAudioTranscriptionModel: OpenAiAudioTranscriptionModel,
    val mcpSyncClients: List<McpSyncClient>,
    val remoteChatClient: ChatClient,
    val orderService: ToolCallback
) {

    @GetMapping("/ai/stream")
    fun simplePrompt(@RequestParam("message") message: String): Flux<String> =
        remoteChatClient.prompt()
            .user(message)
            .stream()
            .content()


    @GetMapping("/ai/top-dishes-per-kitchen")
    fun simplePromptWithConversion(@RequestParam("kitchen") kitchen: String): Dishes? =
        remoteChatClient.prompt()
            .user{
                it.text("Select the most wanted dishes for the following kitchen: {kitchen} with the main ingredients")
                .param("kitchen", kitchen)}
            .call()
            .entity<Dishes?>()



    @GetMapping("/ai/media-prompt")
    fun mediaPrompt(@RequestParam("url") url: URL): Flux<String> =
        remoteChatClient.prompt()
            .user{it.text("Detect all the objects in the image")
                .media(MimeTypeUtils.IMAGE_JPEG, url)
            }
            .stream()
            .content()


    /**
     * Important: using tools via an mcp-server implies that the connection
     * to the mcp-server is stateful. As such such an endpoint won't scale
     * in a concurrent environment. mcp-servers should only be used
     * on the client side, where dedicated, stateful connections to
     * a mcp-server are fine.
     */
    @PostMapping("/ai/chat-with-mcp-server")
    fun chatNoTools(@RequestBody chatInput: ChatInput): String? {
        return this.remoteChatClient
            .prompt()
            .system(MCP_PROMPT)
            .user(chatInput.message)
            .advisors{
                it.param(CONVERSATION_ID, chatInput.conversationId)}
            .tools(SyncMcpToolCallbackProvider(mcpSyncClients))
            .call()
            .content()
    }



    @PostMapping("/ai/chat")
    fun chat(@RequestBody chatInput: ChatInput): String? {
        val relatedDocuments = vectorStore
            .similaritySearch(chatInput.message).orEmpty()
        return this.remoteChatClient
            .prompt()
            .system(SYSTEM_PROMPT)
            .user(createPrompt(chatInput.message, relatedDocuments))
            .advisors{
                it.param(CONVERSATION_ID, chatInput.conversationId) }
            .toolCallbacks(orderService)
            .call()
            .content()
    }


    @PostMapping("/ai/speech")
    fun speech(@RequestBody chatInput: ChatInput): ByteArray {
        val text = chat(chatInput)
        return openAiAudioSpeechModel.call(text)
    }

    @PostMapping("/ai/audio-chat", consumes = ["multipart/form-data"], produces = ["application/octet-stream"])
    fun audioChat(@RequestParam("audio") audioFile: MultipartFile, @RequestParam("conversationId", required = false) conversationId: String? = null): ByteArray {
        // 1. Transcribe audio to text
        val transcriptionPrompt = AudioTranscriptionPrompt(InputStreamResource(audioFile.inputStream))
        val transcriptionResponse = openAiAudioTranscriptionModel.call(transcriptionPrompt)
        val transcribedText = transcriptionResponse.result.output

        // 2. Call the chat method with the transcribed text
        val chatInput = ChatInput(transcribedText, conversationId ?: UUID.randomUUID().toString())
        val chatResponse = chat(chatInput)

        // 3. Convert the response to audio
        val speechPrompt = SpeechPrompt(chatResponse ?: "I couldn't understand that. Please try again.")
        val speechResponse = openAiAudioSpeechModel.call(speechPrompt)
        return speechResponse.result.output
    }


    @GetMapping("/ai/prompt-classifier")
    fun classifyPrompt(@RequestParam("prompt") prompt: String): PromptClassification? =
        remoteChatClient.prompt()
            .user{
                it.text("Classify the following prompt as 'food' or 'other'. If it is about food, extract dish name and/or ingredients: {prompt}")
                    .param("prompt", prompt)}
            .call()
            .entity<PromptClassification?>()



    @GetMapping("/ai/find-dishes")
    fun menuSelection(@RequestParam("foodElements") foodElements: List<String>): List<String> =
     vectorStore
            .similaritySearch(foodElements.joinToString()).orEmpty().mapNotNull { it.text }



    @PostMapping("/ai/order-dish")
    fun orderMeal(@RequestBody order: OrderRequest): OrderResponse =
        orderService(order)



    enum class Classification {
        FOOD, OTHER
    }

    data class PromptClassification(val classification: Classification, val foodElements: List<String>)


    companion object {
        const val SYSTEM_PROMPT = """
            You are an Italian waiter. Respond in a friendly, helpful manner always in English.
    
            Objective: Assist the customer in choosing and ordering the best matching meal based on given food preferences.
    
            Initial Greeting: Always start the initial conversation with: 'Welcome to Italian DelAIght! How can I help you today?'. Don't use this phrase later in the conversation. 
    
            Food Preferences: The customer  will provide food preferences, such as specific dishes like Ravioli or Spaghetti, or ingredients like Cheese or Cream.
    
            Dish Suggestions:
            Only if the user input is about food preferences use the context provided in the user message under 'Dish Context'.
            Only propose dishes from this context; do not invent dishes yourself. Propose ALL the possible options from the context, but at least 3.
            Assist the customer in choosing one of the proposed dishes or encourage him/her to adjust their food preferences if needed.
    
            Order:
            When the client has made a choice trigger the 'orderService' function.
    
            Once the function is successfully called, close the conversation with: "Thank you for your order"
    
            Then summarize the ordered dishes without mentioning the ingredients and give a
            time indication in minutes as returned by the 'orderService' function.
        """

        val AUGMENTED_USER_PROMPT = """       
           User Query:
           {query}

            Dish context:
            {context}"""


        val MCP_PROMPT = """ You are an Italian waiter AI who assists customers in choosing and ordering dishes.
            Here's how to behave:
            - If the user wants to have some ideas about dishes run `complete-menu-italian-delaight-restaurant` to have the complete menu.
            - If the user gives food preferences or ingredients, use the `find-dishes-service` tool to find matching dishes.
            - before looking for preferred meals first run the `classify-prompt-if-food-or-other` tool to understand whether the prompt is about a food preference or not.
            - Propose ALL matching dishes from the `find-dishes-service` result.
            - If the customer confirms a dish, call the `order-dish-service` tool.
            - Once the order is placed, thank them and summarize the dish names with the estimated delivery time.
            
            Only use the tools to gather or act on information. Do not invent dishes. Be polite, helpful, and speak in a friendly English tone.
            """.trimIndent()

        private fun createPrompt(query: String, context: List<Document>): String {
            val promptTemplate = PromptTemplate(AUGMENTED_USER_PROMPT)
            promptTemplate.add("query", query)
            promptTemplate.add("context", context.map { "Dish: ${it.metadata["Name"] } Dish with Ingredients: ${it.text}" }.joinToString(prefix = "- ", separator = "\n - "))
            return promptTemplate.render()
        }

        inline fun <reified T> ChatClient.CallResponseSpec.entity(): T? =
            entity(object: StructuredOutputConverter<T> by BeanOutputConverter(object:ParameterizedTypeReference<T>(){}, jacksonObjectMapper()) {})
    }

}


data class ChatInput(val message: String, val conversationId: String = UUID.randomUUID().toString())

@JsonIgnoreProperties(ignoreUnknown = true)
data class Dishes(val dishes:List<Dish>)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Dish (val dish: String, val ingredients: List<String>)
