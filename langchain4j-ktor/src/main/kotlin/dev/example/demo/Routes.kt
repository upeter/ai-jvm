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
        TODO()
    }

}


suspend fun RoutingContext.missingParameterReply() = call.respondText(
    text = "Missing message parameter",
    status = HttpStatusCode.BadRequest
)
