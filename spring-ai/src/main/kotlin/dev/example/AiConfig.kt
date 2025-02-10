package dev.example

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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
    @Primary
    fun objectMapper(): ObjectMapper = jacksonObjectMapper()


    @Bean
    fun openAiAudioApi(@Value("#{environment.OPENAI_API_KEY}") key: String, restClientBuilder: RestClient.Builder) =
        OpenAiAudioApi(
            "https://api.openai.com", key, restClientBuilder, RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER
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






    @Bean
    fun restClientCustomizer(): RestClientCustomizer {
        val clientConfig: HttpClientConfig = HttpClientConfig.builder().logRequests(true).logResponses(true).build()

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
                val df = DataFrame.read("./langchain4j/src/main/resources/food/italian_delaight_dishes.csv")
                val documents = df.map {
                    runCatching {
                        val ingredients = it["Ingredients"].toString().replace("'", "\"")
                        val content = """${it["Name"]} ${it["Category"]} ${
                            mapper.readValue<List<List<String>>>(ingredients).map { it[0] }
                        }"""
                        Document(content,
                            mapOf(
                                "Name" to it["Name"].toString(),
                                "Category" to it["Category"].toString(),
                                "Ingredients" to it["Ingredients"].toString()
                            )
                        )
                    }.getOrNull()
                }.filterNotNull()

                vectorStore.accept(documents)
                logger.info("Time taken to load Recepies: {} ms", System.currentTimeMillis() - startTime)
            }
        }
    }




    @Bean
    fun orderService(): FunctionCallback {
        return FunctionCallbackWrapper.builder(OrderService())
            .withName("orderService") // (1) function name
            .withDescription("Order meal for customer") // (2) function description
            .withObjectMapper(jacksonObjectMapper())
            .build()
    }

    @Bean
    fun menuService(vectorStore: VectorStore): FunctionCallback {
        return FunctionCallbackWrapper.builder(MenuService(vectorStore))
            .withName("menuService") // (1) function name
            .withDescription("Find matching dishes based on dish name or ingredients") // (2) function description
            .withObjectMapper(jacksonObjectMapper())
            .build()
    }
}

data class OrderRequest(val meals:List<String>)

data class OrderResponse(val deliveredInMinutes:Int)

class OrderService():java.util.function.Function<OrderRequest, OrderResponse> {

    override fun apply(orderRequest: OrderRequest): OrderResponse {
        logger.info(
                "\n*****************************************************************************\n" +
                "🍕🍕🍕 Ordering dishes: ${orderRequest.meals.joinToString("\n- ")} 🍕🍕🍕\n" +
                "*****************************************************************************\n\n")
        return OrderResponse(20)
    }

}


data class MenuRequest(val dish:String)

data class MenuResponse(val menus:List<String>)

class MenuService(val vectorStore: VectorStore):java.util.function.Function<MenuRequest, MenuResponse> {

    override fun apply(dish:MenuRequest): MenuResponse {
        logger.info(
            "\n-------------------------------------------------------------\n" +
                    "🧑‍🍳Calling menu service 🧑‍🍳\n" +
                    "-------------------------------------------------------------\n\n")
        return MenuResponse(vectorStore.similaritySearch(dish.dish).orEmpty().mapNotNull { "Dish: ${it.metadata["Name"] } Dish with Ingredients: ${it.content}" })
    }

}




