package dev.example

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.example.utils.HttpClientConfig
import dev.example.utils.RestClientInterceptor
import io.micrometer.observation.ObservationRegistry
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.map
import org.jetbrains.kotlinx.dataframe.io.read
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.memory.InMemoryChatMemory
import org.springframework.ai.document.Document
import org.springframework.ai.embedding.BatchingStrategy
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.image.ImageModel
import org.springframework.ai.image.ImageOptions
import org.springframework.ai.image.ImageOptionsBuilder
import org.springframework.ai.ollama.OllamaChatModel
import org.springframework.ai.openai.*
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.ai.openai.api.OpenAiAudioApi
import org.springframework.ai.openai.api.OpenAiAudioApi.SpeechRequest.AudioResponseFormat
import org.springframework.ai.openai.api.OpenAiAudioApi.TranscriptResponseFormat
import org.springframework.ai.openai.api.OpenAiImageApi
import org.springframework.ai.retry.RetryUtils
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.function.FunctionToolCallback
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention
import org.springframework.ai.vectorstore.pgvector.PgVectorStore
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.web.client.RestClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.client.BufferingClientHttpRequestFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.web.client.RestClient


@Configuration
class AiConfig {
    @Bean
    fun remoteChatClient(
        openAiChatModel: OpenAiChatModel, chatMemory: ChatMemory,
    ): ChatClient {
        return ChatClient.builder(openAiChatModel)
            .defaultAdvisors(
                SimpleLoggerAdvisor(),
                MessageChatMemoryAdvisor(chatMemory)
            )
            .build()
    }

    @Bean
    fun localChatClient(ollamaChatModel: OllamaChatModel): ChatClient {
        return ChatClient.builder(ollamaChatModel).build()
    }


    @Bean
    @Primary
    fun objectMapper(): ObjectMapper =
        jacksonObjectMapper()

            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)


    @Bean
    fun openAiAudioApi(@Value("#{environment.OPENAI_API_KEY}") key: String, restClientBuilder: RestClient.Builder) =
        OpenAiAudioApi.Builder().baseUrl("https://api.openai.com").apiKey(key).restClientBuilder(restClientBuilder)
            .responseErrorHandler(RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER).build()


    @Bean
    fun transcriptionOptions(): OpenAiAudioTranscriptionOptions {
        return OpenAiAudioTranscriptionOptions.builder()
            .language("en")
            .prompt("Create transcription for this audio file.")
            .temperature(0f)
            //.responseFormat(OpenAiAudioApi.TranscriptResponseFormat.TEXT)
            .model("whisper-1")
            .responseFormat(TranscriptResponseFormat.JSON)
            .build()
    }


    @Bean
    fun transcriptionModel(openAiAudioApi: OpenAiAudioApi, transcriptionOptions: OpenAiAudioTranscriptionOptions) =
        OpenAiAudioTranscriptionModel(openAiAudioApi, transcriptionOptions)


    @Bean
    fun speachOptions(): OpenAiAudioSpeechOptions = OpenAiAudioSpeechOptions.builder()
        .model(OpenAiAudioApi.TtsModel.TTS_1.getValue())
        .responseFormat(AudioResponseFormat.MP3)
        .voice(OpenAiAudioApi.SpeechRequest.Voice.ALLOY)
        .speed(1.0f)
        .build()

    @Bean
    fun speechModel(openAiAudioApi: OpenAiAudioApi, speechOptions: OpenAiAudioSpeechOptions) =
        OpenAiAudioSpeechModel(openAiAudioApi, speechOptions)

    @Bean
    fun imageOptions(): ImageOptions = ImageOptionsBuilder.builder()
        .model("dall-e-3")
        .height(1024)
        .width(1024)
        .build()

    @Bean
    fun imageModel(@Value("#{environment.OPENAI_API_KEY}") apiKey: String): ImageModel {
        return OpenAiImageModel(OpenAiImageApi.Builder().apiKey(apiKey).build())
    }

    @Bean
    fun restClientCustomizer(): RestClientCustomizer {
        val clientConfig: HttpClientConfig = HttpClientConfig.builder().logRequests(true).logResponses(true).build()

        return RestClientCustomizer { restClientBuilder ->
            val requestFactory = BufferingClientHttpRequestFactory(
                SimpleClientHttpRequestFactory().apply {
                    setConnectTimeout(clientConfig.connectTimeout)
                    setReadTimeout(clientConfig.readTimeout)
                }
            )

            restClientBuilder
                .requestFactory(requestFactory)
                .requestInterceptors { interceptors ->
                    if (clientConfig.logRequests || clientConfig.logResponses) {
                        interceptors.add(RestClientInterceptor())
                    }
                }
        }
    }


    @Bean
    fun chatMemory() = InMemoryChatMemory()

    @Bean
    @Primary
    fun embeddingModel( @Qualifier("openAiEmbeddingModel") embeddingModel: EmbeddingModel): EmbeddingModel {
        return embeddingModel
    }


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
                        Document(
                            content,
                            mapOf(
                                "Name" to it["Name"].toString(),
                                "Category" to it["Category"].toString(),
                                "Ingredients" to it["Ingredients"].toString()
                            )
                        )
                    }.getOrNull()
                }.filterNotNull()

                vectorStore.accept(documents)
                logger.info("Time taken to load Recipies: {} ms", System.currentTimeMillis() - startTime)
            }
        }
    }

    @Bean
    fun menuService(vectorStore: VectorStore): ToolCallback {
        return FunctionToolCallback.builder("menuService", MenuService(vectorStore))
            .inputType(MenuRequest::class.java)
            .description("Find matching dishes based on dish name or ingredients")
            .build()
    }


    @Bean
    fun orderService(): ToolCallback {
        return FunctionToolCallback.builder("orderService", OrderService())
            .inputType(OrderRequest::class.java)
            .description("Order meal for customer")
            .build()
    }
}

data class OrderRequest(val meals: List<String>)

data class OrderResponse(val deliveredInMinutes: Int)

class OrderService() : java.util.function.Function<OrderRequest, OrderResponse> {

    override fun apply(orderRequest: OrderRequest): OrderResponse {
        logger.info(
            "\n*****************************************************************************\n" +
              "🍕🍕🍕 Ordering dishes: ${orderRequest.meals.joinToString("\n- ")} 🍕🍕🍕\n" +
              "*****************************************************************************\n\n"
        )
        return OrderResponse(deliveredInMinutes = 20)
    }
}


data class MenuRequest(val dish: String)

data class MenuResponse(val menus: List<String>)

class MenuService(val vectorStore: VectorStore) : java.util.function.Function<MenuRequest, MenuResponse> {

    override fun apply(dish: MenuRequest): MenuResponse {
        logger.info(
            "\n-------------------------------------------------------------\n" +
            "🧑‍🍳Calling menu service 🧑‍🍳\n" +
            "-------------------------------------------------------------\n\n"
        )
        return MenuResponse(
            vectorStore.similaritySearch(dish.dish).orEmpty()
                .mapNotNull { "Dish: ${it.metadata["Name"]} Dish with Ingredients: ${it.text}" })
    }
}




