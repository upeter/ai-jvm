package dev.example

import com.fasterxml.jackson.annotation.JsonClassDescription
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.example.MockWeatherService.Unit
import dev.example.utils.HttpClientConfig
import dev.example.utils.RestClientInterceptor
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.map
import org.jetbrains.kotlinx.dataframe.io.read
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.memory.InMemoryChatMemory
import org.springframework.ai.document.Document
import org.springframework.ai.image.ImageModel
import org.springframework.ai.image.ImageOptions
import org.springframework.ai.image.ImageOptionsBuilder
import org.springframework.ai.model.function.FunctionCallback
import org.springframework.ai.model.function.FunctionCallbackWrapper
import org.springframework.ai.openai.*
import org.springframework.ai.openai.api.OpenAiAudioApi
import org.springframework.ai.openai.api.OpenAiAudioApi.SpeechRequest.AudioResponseFormat
import org.springframework.ai.openai.api.OpenAiAudioApi.TranscriptResponseFormat
import org.springframework.ai.openai.api.OpenAiImageApi
import org.springframework.ai.openai.api.common.OpenAiApiConstants
import org.springframework.ai.retry.RetryUtils
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.web.client.ClientHttpRequestFactories
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings
import org.springframework.boot.web.client.RestClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Description
import org.springframework.context.annotation.Primary
import org.springframework.http.client.BufferingClientHttpRequestFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.web.client.RestClient


@Configuration
class AiConfig {
    @Bean
    fun chatClient(builder: ChatClient.Builder): ChatClient {
        return builder.build()
    }


    @Bean
    fun openAiAudioApi(@Value("#{environment.OPENAI_API_KEY}") key: String, restClientBuilder: RestClient.Builder) =
        OpenAiAudioApi(
            OpenAiApiConstants.DEFAULT_BASE_URL, key, restClientBuilder, RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER
        )


    @Bean
    fun transcriptionOptions(): OpenAiAudioTranscriptionOptions {
        return OpenAiAudioTranscriptionOptions.builder()
            .withLanguage("en")
            .withPrompt("Create transcription for this audio file.")
            .withTemperature(0f)
            //.withResponseFormat(OpenAiAudioApi.TranscriptResponseFormat.TEXT)
            .withModel("whisper-1")
            .withResponseFormat(TranscriptResponseFormat.JSON)
            .build()
    }


    @Bean
    fun transcriptionModel(openAiAudioApi: OpenAiAudioApi, transcriptionOptions: OpenAiAudioTranscriptionOptions) =
        OpenAiAudioTranscriptionModel(openAiAudioApi, transcriptionOptions)


    @Bean
    fun speachOptions(): OpenAiAudioSpeechOptions = OpenAiAudioSpeechOptions.builder()
        .withModel(OpenAiAudioApi.TtsModel.TTS_1.getValue())
        .withResponseFormat(AudioResponseFormat.MP3)
        .withVoice(OpenAiAudioApi.SpeechRequest.Voice.ALLOY)
        .withSpeed(1.0f)
        .build()

    @Bean
    fun speechModel(openAiAudioApi: OpenAiAudioApi, speechOptions: OpenAiAudioSpeechOptions) =
        OpenAiAudioSpeechModel(openAiAudioApi, speechOptions)

    @Bean
    fun imageOptions(): ImageOptions = ImageOptionsBuilder.builder()
        .withModel("dall-e-3")
        .withHeight(1024)
        .withWidth(1024)
        .build()

    @Bean
    fun imageModel(@Value("#{environment.OPENAI_API_KEY}") apiKey: String): ImageModel {
        return OpenAiImageModel(OpenAiImageApi(apiKey))
    }




//    @Bean
//    @Scope("prototype")
//    fun restClientBuilder(restClientBuilderConfigurer: RestClientBuilderConfigurer): RestClient.Builder {
//        val builder = RestClient.builder()
//            .requestFactory(ClientHttpRequestFactories.get(ClientHttpRequestFactorySettings.DEFAULTS))
//            .requestInterceptor(RestClientInterceptor())
//        return restClientBuilderConfigurer.configure(builder);
//    }

    @Bean
    fun restClientCustomizer(): RestClientCustomizer {
        val clientConfig: HttpClientConfig = HttpClientConfig.builder().logRequests(true).logResponses(true).build()
//            .connectTimeout(httpClientProperties.getConnectTimeout())
//            .readTimeout(httpClientProperties.getReadTimeout())
//            .sslBundle(httpClientProperties.getSslBundle())
//            .logRequests(httpClientProperties.isLogRequests())
//            .logResponses(httpClientProperties.isLogResponses())
//            .build()

        return RestClientCustomizer { restClientBuilder ->
            restClientBuilder
                .requestFactory(
                    BufferingClientHttpRequestFactory(
                        ClientHttpRequestFactories.get(
                            ClientHttpRequestFactorySettings.DEFAULTS
                                .withConnectTimeout(clientConfig.connectTimeout)
                                .withReadTimeout(clientConfig.readTimeout)
                        )
                    )
                )
                .requestInterceptors { interceptors ->
                    if (clientConfig.logRequests || clientConfig.logResponses) {
                        interceptors.add(RestClientInterceptor()
//                            HttpLoggingInterceptor(
//                                clientConfig.logRequests,
//                                clientConfig.logResponses
//                            )
                        )
                    }
                }
        }
    }


    @Bean
    fun chatMemory() = InMemoryChatMemory()

    /**
     * https://dev.to/mcadariu/springai-llama3-and-pgvector-bragging-rights-2n8o
     */
    @Bean
    fun applicationRunner(
        jdbcTemplate: JdbcTemplate,
        vectorStore: VectorStore,
        mapper: ObjectMapper,
    ): ApplicationRunner {
        return ApplicationRunner { args: ApplicationArguments? ->
            val sql = "SELECT count(*) FROM vector_store"
            val count = jdbcTemplate.queryForObject<Int>(sql)
            val startTime = System.currentTimeMillis()
            if (count == 0) {
                val df = DataFrame.read("/Users/urs/development/github/ai/kotlin-ai-talk/langchain4j/src/main/resources/food/recipe_selection_en.csv")
                val documents = df.map {
                    runCatching {
                        val ingredients = it["Ingredients"].toString().replace("'", "\"")
                        val content = """${it["Name"]} ${it["Category"]} ${
                            mapper.readValue<List<List<String>>>(ingredients).map { it[0] }
                        }"""
                        Document.builder().withContent(content).withMetadata(
                            mapOf(
                                "Name" to it["Name"].toString(),
                                "Category" to it["Category"].toString(),
                                "Ingredients" to it["Ingredients"].toString()
                            )
                        ).build()
                    }.getOrNull()
                }.filterNotNull()

                vectorStore.accept(documents)
                logger.info("Time taken to load Recepies: {} ms", System.currentTimeMillis() - startTime)
            }
        }
    }


    @Bean
    @Description("Get the weather in location") // function description
    fun weatherService(): java.util.function.Function<Request, Response> {
        return MockWeatherService()
    }

    @Bean
    @Primary
    fun objectMapper(): ObjectMapper {
        return jacksonObjectMapper()
    }



//    @Bean
//    @Description("Order food to given location")
//    fun foodOrderService(): java.util.function.Function<OrderFoodRequest, OrderFoodResponse> =
//        AnimalCatchService()

    @Bean
    fun foodOrderService(): FunctionCallback {
        return FunctionCallbackWrapper.builder(AnimalCatchService())
            .withName("foodOrderService") // (1) function name
            .withDescription("Order food to given location") // (2) function description
            .withObjectMapper(jacksonObjectMapper())
            .build()
    }


}





//@JsonClassDescription("Order food to location")
data class OrderFoodRequest(val latitude:String, val longitude:String)
data class OrderFoodResponse(val ok:Boolean)


class AnimalCatchService():java.util.function.Function<OrderFoodRequest, OrderFoodResponse> {

    override fun apply(location: OrderFoodRequest): OrderFoodResponse {
        logger.info("Catching Animal at location: $location")
        return OrderFoodResponse(true)
    }

}


@JvmRecord
@JsonClassDescription("Get the weather in location")
data class Request(val location: String, val unit: Unit)

@JvmRecord
data class Response(val temp: Double, val unit: Unit)

class MockWeatherService : java.util.function.Function<Request, Response> {
    enum class Unit {
        C, F
    }

    override fun apply(request: Request): Response {
        return Response(30.0, Unit.C)
    }
}


