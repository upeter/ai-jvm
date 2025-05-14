package dev.example.demo

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Configure the AI routes
 */
fun Route.aiRoutes(
    chatLanguageModel: ChatLanguageModel,
    streamingChatLanguageModel: StreamingChatLanguageModel) {

    get("/ai/ask") {
        val message = call.request.queryParameters["message"] ?: return@get missingParameterReply()
        val response = chatLanguageModel.generate(message)
        call.respondText(response)

    }

    get("/ai/ask/stream") {
        val message = call.request.queryParameters["message"] ?: return@get missingParameterReply()
        val responseFlow = streamingChatLanguageModel.generateFlow(message)
        call.respondTextWriter {
            responseFlow.collect { chunk ->
                write(chunk)
                flush()
            }
        }
    }


}


suspend fun RoutingContext.missingParameterReply() = call.respondText(
    text = "Missing message parameter",
    status = HttpStatusCode.BadRequest
)
