package dev.example.demo

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.serialization.jackson.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.http.*
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.slf4j.event.Level

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
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
            call.respondText("500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }

    // Configure OpenAI models
    val openAiConfig = environment.config.config("langchain4j.openai")
    val chatLanguageModel = configureChatLanguageModel(openAiConfig.config("chat-model"))
    val streamingChatLanguageModel = configureStreamingChatLanguageModel(openAiConfig.config("streaming-chat-model"))

    // Configure routing
    routing {
        aiRoutes(chatLanguageModel, streamingChatLanguageModel)
    }
}
