package dev.example.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.AdvisedRequest
import org.springframework.ai.chat.client.RequestResponseAdvisor
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.MessageAggregator
import org.springframework.ai.model.ModelOptionsUtils
import reactor.core.publisher.Flux
import java.util.function.Function


class SimpleLoggerAdvisor @JvmOverloads constructor(
    private val requestToString: Function<AdvisedRequest, String?> = DEFAULT_REQUEST_TO_STRING,
    private val responseToString: Function<ChatResponse?, String?> = DEFAULT_RESPONSE_TO_STRING
) :
    RequestResponseAdvisor {
    override fun adviseRequest(request: AdvisedRequest, context: Map<String, Any>): AdvisedRequest {
        logger.debug("request: {}", requestToString.apply(request))
        return request
    }

    override fun adviseResponse(fluxChatResponse: Flux<ChatResponse>, context: Map<String, Any>): Flux<ChatResponse> {
        return (MessageAggregator()).aggregate(
            fluxChatResponse
        ) { chatResponse: ChatResponse? ->
            logger.debug(
                "stream response: {}",
                responseToString.apply(chatResponse)
            )
        }
    }

    override fun adviseResponse(response: ChatResponse, context: Map<String, Any>): ChatResponse {
        logger.debug("response: {}", responseToString.apply(response))
        return response
    }

    override fun toString(): String {
        return SimpleLoggerAdvisor::class.java.simpleName
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(SimpleLoggerAdvisor::class.java)
        val DEFAULT_REQUEST_TO_STRING: Function<AdvisedRequest, String?> =
            Function { request: AdvisedRequest -> request.toString() }
        val DEFAULT_RESPONSE_TO_STRING: Function<ChatResponse?, String?> =
            Function { response: ChatResponse? -> ModelOptionsUtils.toJsonString(response) }
    }
}