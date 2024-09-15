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
import java.util.UUID

@RestController
internal class AIController(chatClientBuilder: ChatClient.Builder, val vectorStore: VectorStore, chatMemory: ChatMemory, val openAiAudioSpeechModel: OpenAiAudioSpeechModel) {

    private val chatClient = chatClientBuilder.defaultAdvisors( SimpleLoggerAdvisor(), MessageChatMemoryAdvisor(chatMemory)).build()


    @PostMapping("/ai/stream")
    fun streamCompletion(@RequestBody chatInput: ChatInput): Flow<String> =
        chatClient.prompt()
            .user(chatInput.message)
            .stream()
            .content()
            .asFlow()




    companion object {
        val SYSTEM_PROMPT = """
           You are an Italian waiter. Respond in a friendly, helpful, crisp, and joyful manner.

            Objective: Find the best matching meal for a cat based on its food preferences.
            
            Initial Greeting: 
            At the very beginning, start with: "Hey cat, you look hungry. Can I order a delicious meal for you? Tell me what you want." Never repeat this phrase later in the conversations.
            
            Food Preferences: 
            The cat will supply food preferences, such as specific dishes like Ravioli or Spaghetti, or ingredients like Cheese or Cream.
            
            Dish Suggestions:
            Only if the user query is about dishes or ingredients, then use the dish context provided in the user message under 'Dish Context'. 
            Otherwise ignore the dish context entirely and use the history.
            Only propose dishes from this context; do not invent dishes yourself.
            Assist the cat in choosing one of the proposed dishes or encourage them to adjust their food preferences if needed.
            
            Order Confirmation:
            Before proceeding, ensure the cat agrees on one dish.
            Once the dish is chosen, summarize the order without mentioning the ingredients.
            Close the conversation with: "I will order these for you, stay put!"
            
            
           """

        val USER_PROMPT = """
            Dish Context:
            {context}
            
            User Query:
            {query}
            """

        private fun createPrompt(query: String, context: List<Document>, latitude: String = "", longitude: String = ""): String {
            val promptTemplate = PromptTemplate(USER_PROMPT)
            promptTemplate.add("query", query)
            promptTemplate.add("latitude", latitude)
            promptTemplate.add("longitude", longitude)
            promptTemplate.add("context", context.map { "Dish: ${it.metadata["Name"] } Dish with Ingredients: ${it.content}" }.joinToString(prefix = "- ", separator = "\n - "))
            return promptTemplate.render()
        }

    }

}


data class ChatInput(val message: String, val conversationId: String = UUID.randomUUID().toString(), val latitude:String = "40.730610",  val longitude:String = "-73.935242")

