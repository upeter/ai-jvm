package dev.example.demo

import dev.langchain4j.data.message.UserMessage.userMessage
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.StreamingChatModel
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.Flow
import me.kpavlov.langchain4j.kotlin.model.chat.StreamingChatModelReply
import me.kpavlov.langchain4j.kotlin.model.chat.chatFlow

/**
 * Configure the AI routes
 */
fun Route.aiRoutes(
    chatLanguageModel: ChatModel,
    streamingChatLanguageModel: StreamingChatModel
) {

    get("/ai/ask") {
        val message = call.request.queryParameters["message"] ?: return@get missingParameterReply()
        val response = chatLanguageModel.chat(message)
        call.respondText(response)

    }

    get("/ai/ask/stream") {
        val message = call.request.queryParameters["message"] ?: return@get missingParameterReply()
        val responseFlow: Flow<StreamingChatModelReply> = streamingChatLanguageModel.chatFlow { messages += userMessage(message) }
        call.respondTextWriter {
            responseFlow.collect { reply ->
                when (reply) {
                    is StreamingChatModelReply.CompleteResponse ->
                        write(reply.response.aiMessage().text())
                    is StreamingChatModelReply.PartialResponse ->
                        write(reply.token)
                    is StreamingChatModelReply.Error -> throw IllegalArgumentException("Error reply: $reply")
                }
                flush()
            }
        }
    }


}


suspend fun RoutingContext.missingParameterReply() = call.respondText(
    text = "Missing message parameter",
    status = HttpStatusCode.BadRequest
)
