package dev.example.demo

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.StreamingResponseHandler
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import kotlinx.coroutines.reactive.asFlow
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Sinks
import kotlinx.coroutines.flow.Flow

@RestController
class AIController(
    val chatLanguageModel: ChatLanguageModel,
    val streamingChatModel: StreamingChatLanguageModel,
    val assistant: Assistant,
    val streamingAssistant: StreamingAssistant,
) {

    @GetMapping("/ai/stream")
    fun streamCompletion(@RequestParam("message") message:String): Flow<String> {
        return streamingAssistant.chat(message).asFlow()
    //return streamingChatModel.generateStream(UserMessage(message))
    }

//chatLanguageModel.generate(UserMessage(message)).content().text()
        //assistant.chat(message).also { println(it) }




}

fun StreamingChatLanguageModel.generateStream(message: UserMessage):Flow<String> {
    val sink: Sinks.Many<String> = Sinks.many().unicast().onBackpressureBuffer()
    this.generate(message, object : StreamingResponseHandler<AiMessage> {
        override fun onNext(token: String) {
            sink.tryEmitNext(token)
        }

        override fun onError(error: Throwable) {
            sink.tryEmitError(error)
        }
    })

    return sink.asFlux().asFlow()
}

