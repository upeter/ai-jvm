package dev.example.demo

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.serialization.jackson.*
import io.ktor.server.routing.*
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.slf4j.event.Level

fun main(args: Array<String>) {
    embeddedServer(Netty, commandLineEnvironment(args)).start(wait = true)
}

@Suppress("unused") // Referenced in application.conf
fun Application.module() {
    // Configure plugins
    install(CallLogging) {
        level = Level.INFO
    }
    
    install(ContentNegotiation) {
        jackson {
            registerKotlinModule()
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }
    
    install(CORS) {
        anyHost()
    }
    
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = io.ktor.http.HttpStatusCode.InternalServerError)
        }
    }
    
    // Configure OpenAI models
    val openAiConfig = environment.config.config("langchain4j.openai")
    val chatLanguageModel = configureChatLanguageModel(openAiConfig.config("chat-model"))
    val streamingChatLanguageModel = configureStreamingChatLanguageModel(openAiConfig.config("streaming-chat-model"))
    
    // Create assistants
    val assistant = Assistant(chatLanguageModel)
    val streamingAssistant = StreamingAssistant(streamingChatLanguageModel)
    
    // Configure routing
    routing {
        aiRoutes(assistant, streamingAssistant)
    }
}