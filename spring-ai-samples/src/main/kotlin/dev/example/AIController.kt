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


    @PostMapping("/ai/chat")
    fun chat(@RequestBody chatInput: ChatInput): String {
        val relatedDocuments: List<Document> = vectorStore.similaritySearch(chatInput.message)
        return this.chatClient
            .prompt()
            .system(SYSTEM_PROMPT)
            .user(createPrompt(chatInput.message, relatedDocuments))
            .advisors{it.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatInput.conversationId).param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 200)}
            .functions("menuService", "orderService")
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
            You are an Italian waiter. Respond in a friendly, helpful manner.

            Objective: Assist the customer in choosing and ordering the best matching meal based on given food preferences.
            
            Food Preferences: The customer  will provide food preferences, such as specific dishes like Ravioli or Spaghetti, or ingredients like Cheese or Cream.
            
            Dish Suggestions:         
            Classify Input:         
            Determine whether the prompt represents a dish, food, or ingredient preference (Yes or No).
            
            If Yes (New Preferences):       
            Call the 'menuService' with the original prompt stripped of all non-food-related content.
            Use only the reply from the 'menuService' to propose dishes; do not invent dishes yourself.
            Assist the customer in choosing one of the proposed dishes or encourage them to adjust their food preferences if needed.
            Important: If the customer is confirming or choosing one of the previously proposed dishes, do not call the 'menuService' again, even if their prompt includes dish names or ingredients.
            
            If No:            
            Politely ask the customer to specify their food preferences or suggest some ingredients or dishes they like.
                       
            Order Confirmation:          
            Acknowledgment:           
            If the customer intends to order one of the proposed dishes, proceed without calling the 'menuService' again.
            
            Summarize Order:          
            Summarize the order without mentioning the ingredients.
           
            Post-Order Actions:          
            After confirming the order, trigger the 'orderService' function.
            Once the function is successfully called, close the conversation with: "Thank you for your order" 
            Then summarize the ordered meals and give a time indication in minutes as returned by the 'orderService' function.
           """

        val USER_PROMPT = """
          
            User Query:
            {query}
            """

        private fun createPrompt(query: String, context: List<Document>): String {
            val promptTemplate = PromptTemplate(USER_PROMPT)
            promptTemplate.add("query", query)
            //promptTemplate.add("context", context.map { "Dish: ${it.metadata["Name"] } Dish with Ingredients: ${it.content}" }.joinToString(prefix = "- ", separator = "\n - "))
            return promptTemplate.render()
        }

    }

}


data class ChatInput(val message: String, val conversationId: String = UUID.randomUUID().toString())

