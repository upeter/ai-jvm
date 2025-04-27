package dev.example.demo

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.output.Response
import dev.langchain4j.model.StreamingResponseHandler
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import org.slf4j.LoggerFactory
import reactor.core.publisher.Sinks

inline val <reified T> T.logger
    get() = LoggerFactory.getLogger(T::class.java)


fun StreamingChatLanguageModel.generateFlow(message: String): Flow<String> {
    val sink: Sinks.Many<String> = Sinks.many().unicast().onBackpressureBuffer()
    this.generate(UserMessage(message), object : StreamingResponseHandler<AiMessage> {
        override fun onNext(token: String) {
            sink.tryEmitNext(token)
        }

        override fun onError(error: Throwable) {
            sink.tryEmitError(error)
        }

        override fun onComplete(response:Response<AiMessage>) {
            sink.tryEmitComplete()
        }
    })

    return sink.asFlux().asFlow()
}

