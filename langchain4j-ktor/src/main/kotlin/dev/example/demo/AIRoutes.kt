package dev.example.demo

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.Flow

/**
 * Configure the AI routes
 */
fun Route.aiRoutes(assistant: Assistant, streamingAssistant: StreamingAssistant) {
    // Simple chat endpoint
    get("/ai/ask") {
        val message = call.request.queryParameters["message"]
            ?: return@get call.respondText(
                "Missing message parameter",
                status = HttpStatusCode.BadRequest
            )
        
        val response = assistant.chat(message)
        call.respondText(response)
    }
    
    // Streaming chat endpoint
    get("/ai/ask/stream") {
        val message = call.request.queryParameters["message"]
            ?: return@get call.respondText(
                "Missing message parameter",
                status = HttpStatusCode.BadRequest
            )
        
        val responseFlow = streamingAssistant.chat(message)
        call.respondTextWriter(contentType = ContentType.Text.Plain) {
            responseFlow.collect { chunk ->
                write(chunk)
                flush()
            }
        }
    }
}