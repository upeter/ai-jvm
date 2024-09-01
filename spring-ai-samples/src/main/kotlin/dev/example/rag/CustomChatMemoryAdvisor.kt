package dev.example.rag

import org.springframework.ai.chat.client.AdvisedRequest
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.MessageType
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.model.MessageAggregator
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import reactor.core.publisher.Flux
import java.util.stream.Collectors

class CustomChatMemoryAdvisor(
    vectorStore: VectorStore, val defaultConversationId: String,
                              val chatHistoryWindowSize: Int,
                              val systemTextAdvise: String = DEFAULT_SYSTEM_TEXT_ADVISE) : AbstractChatMemoryAdvisor<VectorStore>(vectorStore, defaultConversationId, chatHistoryWindowSize) {

    override fun adviseRequest(request: AdvisedRequest, context: Map<String, Any>): AdvisedRequest {
        val advisedSystemText = request.systemText() + System.lineSeparator() + this.systemTextAdvise

        val searchRequest = SearchRequest.query(request.userText())
            .withTopK(this.doGetChatMemoryRetrieveSize(context))
            .withFilterExpression(
                "$DOCUMENT_METADATA_CONVERSATION_ID=='" + this.doGetConversationId(context) + "'"
            )

        val documents =
            getChatMemoryStore().similaritySearch(searchRequest)

        val longTermMemory = documents
            .map { obj: Document -> obj.content }
            .joinToString(System.lineSeparator())

        val advisedSystemParams: MutableMap<String, Any> = HashMap(request.systemParams())
        advisedSystemParams["long_term_memory"] = longTermMemory

        val advisedRequest = AdvisedRequest.from(request)
            .withSystemText(advisedSystemText)
            .withSystemParams(advisedSystemParams)
            .build()

        val userMessage = UserMessage(request.userText(), request.media())
        getChatMemoryStore().write(
            toDocuments(listOf<Message>(userMessage), this.doGetConversationId(context))
        )

        return advisedRequest
    }

    override fun adviseResponse(chatResponse: ChatResponse, context: Map<String, Any>): ChatResponse {
        val assistantMessages = chatResponse.results.map { g: Generation -> g.output as Message }
        getChatMemoryStore().write(toDocuments(assistantMessages, this.doGetConversationId(context)))
        return chatResponse
    }

    override fun adviseResponse(fluxChatResponse: Flux<ChatResponse>, context: Map<String, Any>): Flux<ChatResponse> {
        return MessageAggregator().aggregate(fluxChatResponse) { chatResponse: ChatResponse ->
            val assistantMessages = chatResponse.results
                .map { g: Generation -> g.output as Message }
            getChatMemoryStore().write(toDocuments(assistantMessages, this.doGetConversationId(context)))
        }
    }

    private fun toDocuments(messages: List<Message>, conversationId: String): List<Document> {
        val docs = messages
            .filter { m: Message -> m.messageType == MessageType.USER || m.messageType == MessageType.ASSISTANT }
            .map { message: Message ->
                val metadata = message.metadata.toMutableMap()
                metadata[DOCUMENT_METADATA_CONVERSATION_ID] = conversationId
                metadata[DOCUMENT_METADATA_MESSAGE_TYPE] = message.messageType.name
                val doc = Document(message.content, metadata)
                doc
            }


        return docs
    }

    companion object {
        const val DOCUMENT_METADATA_CONVERSATION_ID = "conversationId"

        const val DOCUMENT_METADATA_MESSAGE_TYPE = "messageType"

        val DEFAULT_SYSTEM_TEXT_ADVISE = """

			Use the long term conversation memory from the LONG_TERM_MEMORY section to provide accurate answers.

			---------------------
			LONG_TERM_MEMORY:
			{long_term_memory}
			---------------------

			

			""".trimIndent()
    }
}
