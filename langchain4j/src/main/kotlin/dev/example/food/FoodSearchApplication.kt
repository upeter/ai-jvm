package dev.example.food
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.langchain4j.data.document.Document
import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.document.splitter.DocumentSplitters
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.memory.chat.MessageWindowChatMemory
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.openai.OpenAiChatModelName
import dev.langchain4j.model.openai.OpenAiTokenizer
import dev.langchain4j.rag.DefaultRetrievalAugmentor
import dev.langchain4j.rag.content.injector.DefaultContentInjector
import dev.langchain4j.rag.content.retriever.ContentRetriever
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever
import dev.langchain4j.service.AiServices
import dev.langchain4j.service.SystemMessage
import dev.langchain4j.store.embedding.EmbeddingStore
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ResourceLoader
import java.util.*
import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.map
import org.jetbrains.kotlinx.dataframe.io.read
import java.util.concurrent.atomic.AtomicInteger
import dev.example.utils.logger

@SpringBootApplication
class ItalianFoodSearchApplication {
    @Bean
    fun interactiveFoodChatRunner(agent: ItalianFoodSupportAgent): ApplicationRunner {
        return ApplicationRunner { _: ApplicationArguments ->
            Scanner(System.`in`).use { scanner ->
                while (true) {
                    print("User: ")
                    val userMessage = scanner.nextLine()
                    if ("exit".equals(userMessage, ignoreCase = true)) {
                        break
                    }
                    val agentMessage = agent.chat(userMessage)
                    println("Agent: $agentMessage")
                }
            }
        }
    }

    @Bean
    fun italianFoodAgent(
        chatLanguageModel: ChatLanguageModel,
        italianFoodContentRetriever: ContentRetriever
    ): ItalianFoodSupportAgent {
        return AiServices.builder(ItalianFoodSupportAgent::class.java)
            .chatLanguageModel(chatLanguageModel)
            .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
            //.contentRetriever(italianFoodContentRetriever)
            .retrievalAugmentor(
                DefaultRetrievalAugmentor.builder()
                    .contentRetriever(italianFoodContentRetriever)
                    .contentInjector(
                        DefaultContentInjector.builder()
                            //.promptTemplate("Category {Category} is")// .promptTemplate(...) // Formatting can also be changed
                            .metadataKeysToInclude(listOf("Category", "Ingredients"))
                            .build()
                    )
                    .build()
            )
            .build()
    }


    @Bean
    fun italianFoodContentRetriever(
        italianFoodEmbeddingStore: EmbeddingStore<TextSegment>,
        embeddingModel: EmbeddingModel
    ): ContentRetriever {
        // You will need to adjust these parameters to find the optimal setting, which will depend on two main factors:
        // - The nature of your data
        // - The embedding model you are using

        val maxResults = 5
        val minScore = 0.6

        return EmbeddingStoreContentRetriever.builder()
            .embeddingStore(italianFoodEmbeddingStore)
            .embeddingModel(embeddingModel)
            .maxResults(maxResults)
            .minScore(minScore)
            .build()
    }


    @Bean
    fun italianFoodEmbeddingStore(
        @Value("\${pgvector.store.host}") host: String,
        @Value("\${pgvector.store.port}") port: Int,
        @Value("\${pgvector.store.user}") user: String,
        @Value("\${pgvector.store.password}") password: String,
        embeddingModel: EmbeddingModel, resourceLoader: ResourceLoader
    ): EmbeddingStore<TextSegment> {
        // Normally, you would already have your embedding store filled with your data.
        // However, for the purpose of this demonstration, we will:

        // 1. Create an in-memory embedding store

        val embeddingStore: EmbeddingStore<TextSegment> =  //InMemoryEmbeddingStore()
            PgVectorEmbeddingStore.builder()
                .host(host)
                .port(port)
                .user(user)
                .password(password)
                .database("langchain")
                .table("italianfood")
                .dimension(384)
                .dropTableFirst(true)
                .build();

        // 2. Load an example document ("Miles of Smiles" terms of use)
        //val resource = resourceLoader.getResource("classpath:miles-of-smiles-terms-of-use.txt")
        //val document = FileSystemDocumentLoader.loadDocument(resource.file.toPath(), TextDocumentParser())


        // 3. Split the document into segments 100 tokens each
        // 4. Convert segments into embeddings
        // 5. Store embeddings into embedding store
        // All this can be done manually, but we will use EmbeddingStoreIngestor to automate this:

        val documentSplitter = DocumentSplitters.recursive(100, 0, OpenAiTokenizer(OpenAiChatModelName.GPT_4_O_MINI))
        val ingestor = EmbeddingStoreIngestor.builder()
            .documentSplitter(documentSplitter)
            .embeddingModel(embeddingModel)
            .embeddingStore(embeddingStore)
            .build()


        val df = DataFrame.read("langchain4j/src/main/resources/food/recipe_selection_en.csv")
        val documents = df.map {
            runCatching {
                val ingredients = it["Ingredients"].toString().replace("'", "\"")
                val content = """${it["Name"]} ${it["Category"]} ${mapper.readValue<List<List<String>>>(ingredients).map { it[0] }}"""
                Document.document(content, Metadata(mapOf(
                            "Category" to it["Category"].toString(),
                            "Ingredients" to it["Ingredients"].toString()
                        )
                    )
                )
            }.getOrNull()
        }.filterNotNull()

        val rows = df.size().nrow
        val counter = AtomicInteger(0)
        documents.stream().parallel().forEach {
            ingestor.ingest(it);
            val count = counter.incrementAndGet()
            if(count % 20 == 0) {
                logger.info("Ingested: $count / $rows")
            }
        }
        return embeddingStore
    }

    @Bean
    fun embeddingModel(): EmbeddingModel {
        return AllMiniLmL6V2EmbeddingModel()
    }

    companion object {
        val mapper = jacksonObjectMapper()
    }


}


interface ItalianFoodSupportAgent {
    @SystemMessage(
        "You are an Italian Food adviser. You have to find the best matching recipie based on food preference of a user. Food preference can be: a concrete meal, ingredients, kind of meal (starter, main dish, dissert)",
        //"Crucial in your dialog is the disorder and herbal drugs to heal them or the herbal drug itself and it's healing qualities.",

    )
    fun chat(userMessage: String): String
}

fun main(args: Array<String>) {
    SpringApplication.run(ItalianFoodSearchApplication::class.java, *args)
}

