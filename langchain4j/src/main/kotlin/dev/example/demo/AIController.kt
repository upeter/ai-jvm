package dev.example.demo

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import dev.langchain4j.service.SystemMessage
import dev.langchain4j.service.spring.AiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
class AIController(
    val chatLanguageModel: ChatLanguageModel,
    val assistant: Assistant,
    val streamingAssistant: StreamingAssistant) {

    @GetMapping("/ai/ask")
    fun simpleChat(@RequestParam("message") message:String): String =
        assistant.chat(message)

    @GetMapping("/ai/ask/stream")
    fun streamChat(@RequestParam("message") message:String): Flow<String> =
        streamingAssistant.chat(message).asFlow()

}

@AiService
fun interface StreamingAssistant {
    @SystemMessage("You are a polite assistant")
    fun chat(userMessage: String): Flux<String>
}


@AiService
fun interface Assistant {
    @SystemMessage("You are a polite assistant")
    fun chat(userMessage: String): String
}


