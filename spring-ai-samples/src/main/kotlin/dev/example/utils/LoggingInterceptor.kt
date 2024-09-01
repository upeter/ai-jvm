package dev.example.utils

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.example.logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.lang.Nullable
import org.springframework.util.Assert
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.time.Duration

/**
 * Options for configuring the HTTP clients used by the models.
 */
@JvmRecord
data class HttpClientConfig(
    val connectTimeout: Duration,
    val readTimeout: Duration,
    @field:Nullable @param:Nullable val sslBundle: String?,
    val logRequests: Boolean,
    val logResponses: Boolean,
) {
    class Builder internal constructor() {
        private var connectTimeout: Duration = Duration.ofSeconds(10)
        private var readTimeout: Duration = Duration.ofSeconds(60)

        @Nullable
        private var sslBundle: String? = null
        private var logRequests = false
        private var logResponses = false

        fun connectTimeout(connectTimeout: Duration): Builder {
            this.connectTimeout = connectTimeout
            return this
        }

        fun readTimeout(readTimeout: Duration): Builder {
            this.readTimeout = readTimeout
            return this
        }

        fun sslBundle(sslBundle: String?): Builder {
            this.sslBundle = sslBundle
            return this
        }

        fun logRequests(logRequests: Boolean): Builder {
            this.logRequests = logRequests
            return this
        }

        fun logResponses(logResponses: Boolean): Builder {
            this.logResponses = logResponses
            return this
        }

        fun build(): HttpClientConfig {
            return HttpClientConfig(connectTimeout, readTimeout, sslBundle, logRequests, logResponses)
        }
    }

    init {
        Assert.notNull(connectTimeout, "connectTimeout must not be null")
        Assert.notNull(readTimeout, "readTimeout must not be null")
    }

    companion object {
        fun builder(): Builder {
            return Builder()
        }
    }
}

/**
 * HTTP interceptor for logging requests and responses.
 */
class HttpLoggingInterceptor(private val logRequests: Boolean, private val logResponses: Boolean) :
    ClientHttpRequestInterceptor {
    override fun intercept(
        request: HttpRequest,
        requestBody: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        if (logRequests) {
            logRequest(request, requestBody)
        }

        return if (logResponses) {
            logResponse(request, requestBody, execution)
        } else {
            execution.execute(request, requestBody)
        }
    }

    private fun logRequest(request: HttpRequest, requestBody: ByteArray) {
        logger.info("Request.\n Method: {}.\n URI: {}.\n Headers: {}.\n Body: {}", request.getMethod(),
            request.getURI(),
            request.getHeaders()
                .filter { e -> !e.key.equals("authorizaion", true) }
                .map { e -> e.key + ":" + e.value }
                .joinToString(", "),

            String(requestBody, StandardCharsets.UTF_8))
    }

    @Throws(IOException::class)
    private fun logResponse(
        request: HttpRequest, requestBody: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        val response: ClientHttpResponse = execution.execute(request, requestBody)
        val responseBody: String = response.body.bufferedReader().readText()

        logger.info("Response.\n Status Code: {}.\n Headers: {}.\n Body: {}", response.getStatusText(),
            response.getHeaders()
                .map { e -> e.key + ":" + e.value }
                .joinToString(", "),
            responseBody)

        return response
    }

}

class RestClientInterceptor : ClientHttpRequestInterceptor {

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        logRequest(request, body)
        val response = execution.execute(request, body)
        val responseWrapper = BufferingClientHttpResponseWrapper(response)
        logResponse(responseWrapper)
        return responseWrapper
    }

    fun logRequest(request: HttpRequest, bytes: ByteArray) {

        val request = "Request:\n\tRequest: ${request.method} ${request.uri}\n" +
        "\tHeaders: ${request.headers}\n" +
        "\tBody: ${bytes.toJsonOrRaw()}"
        logger.info(request)
    }

    fun logResponse(response: ClientHttpResponse) {
        val response = "Response:\n\tResponse: ${response.statusCode} ${response.statusText}\n" +
        "\tHeaders: ${response.headers}\n" +
        "\tBody: ${response.body.reader().readText()}"
        logger.info(response)
    }

    // A Groovy-ier version of:
    // https://github.com/spring-projects/spring-framework/blob/main/spring-web/src/main/java/org/springframework/http/client/BufferingClientHttpResponseWrapper.java
    class BufferingClientHttpResponseWrapper(val response: ClientHttpResponse) : ClientHttpResponse by response {

        var body: ByteArray? = null

        override fun getBody(): InputStream {
            if (this.body == null) {
                this.body = response.body.readAllBytes()
            }
            return ByteArrayInputStream(this.body)
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(RestClientInterceptor::class.java)
        val mapper = jacksonObjectMapper()
        fun ByteArray.toJsonOrRaw(): String = try {
            mapper.readTree(this).toPrettyString()
        } catch (e: Exception) {
            String(this)
        }

    }


}