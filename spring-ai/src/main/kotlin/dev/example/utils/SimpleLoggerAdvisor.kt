package dev.example.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisor
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisorChain
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.MessageAggregator
import org.springframework.ai.model.ModelOptionsUtils
import reactor.core.publisher.Flux
import java.util.function.Function


class SimpleLoggerAdvisor @JvmOverloads constructor(
    private val requestToString: Function<AdvisedRequest, String?> = DEFAULT_REQUEST_TO_STRING,
    private val responseToString: Function<ChatResponse?, String?> = DEFAULT_RESPONSE_TO_STRING
) :
    CallAroundAdvisor, StreamAroundAdvisor {

    override fun aroundCall(
        advisedRequest: AdvisedRequest,
        chain: CallAroundAdvisorChain,
    ): AdvisedResponse {
            logger.debug("request: {}", requestToString.apply(advisedRequest))
            return chain.nextAroundCall(advisedRequest).also {
                logger.debug("response: {}", responseToString.apply(it.response))
            }
    }


    override fun aroundStream(
        advisedRequest: AdvisedRequest,
        chain: StreamAroundAdvisorChain,
    ): Flux<AdvisedResponse?> {
        val response = chain.nextAroundStream(advisedRequest)
        return (MessageAggregator()).aggregateAdvisedResponse(response) { adviseResponse ->
            logger.debug("stream response: {}", adviseResponse)
        }
    }


    override fun toString(): String {
        return SimpleLoggerAdvisor::class.java.simpleName
    }
    override fun getName(): String  =  SimpleLoggerAdvisor::class.java.simpleName

    override fun getOrder(): Int = 0


    companion object {
        private val logger: Logger = LoggerFactory.getLogger(SimpleLoggerAdvisor::class.java)
        val DEFAULT_REQUEST_TO_STRING: Function<AdvisedRequest, String?> =
            Function { request: AdvisedRequest -> request.toString() }
        val DEFAULT_RESPONSE_TO_STRING: Function<ChatResponse?, String?> =
            Function { response: ChatResponse? -> ModelOptionsUtils.toJsonString(response) }
    }
}