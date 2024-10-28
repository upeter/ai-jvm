package dev.example.demo

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
class AIController(
    val chatLanguageModel: ChatLanguageModel,
    val streamingChatModel: StreamingChatLanguageModel,
    val assistant: Assistant,
    val streamingAssistant: StreamingAssistant,
) {

    @GetMapping("/ai/ask")
    fun simpleChat(@RequestParam("message") message:String): String {
        return chatLanguageModel.generate(message)
    }

    @GetMapping("/ai/stream")
    fun streamChat(@RequestParam("message") message:String): Flux<String> {
        return streamingAssistant.chat(message)
    }


}



