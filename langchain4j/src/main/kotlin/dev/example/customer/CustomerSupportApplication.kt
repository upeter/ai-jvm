package dev.example.customer

import dev.langchain4j.data.document.loader.FileSystemDocumentLoader
import dev.langchain4j.data.document.parser.TextDocumentParser
import dev.langchain4j.data.document.splitter.DocumentSplitters
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.memory.chat.MessageWindowChatMemory
import dev.langchain4j.model.chat.ChatLanguageModel
//import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.openai.OpenAiModelName
import dev.langchain4j.model.openai.OpenAiTokenizer
import dev.langchain4j.rag.content.retriever.ContentRetriever
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever
import dev.langchain4j.service.AiServices
import dev.langchain4j.service.SystemMessage
import dev.langchain4j.store.embedding.EmbeddingStore
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ResourceLoader
import java.util.*

@SpringBootApplication
class CustomerSupportApplication {
    /**
     * Run CustomerSupportApplicationTest to see simulated conversation with customer support agent
     */
    @Bean
    fun interactiveChatRunner(customerAgent: CustomerSupportAgent): ApplicationRunner {
        return ApplicationRunner { args: ApplicationArguments ->
            val scanner = Scanner(System.`in`)
            while (true) {
                print("User: ")
                val userMessage = scanner.nextLine()

                if ("exit".equals(userMessage, ignoreCase = true)) {
                    break
                }


                val agentMessage = customerAgent.chat(userMessage)
                println("Agent: $agentMessage")
            }
            scanner.close()
        }
    }

    @Bean
    fun customerAgent(
        chatLanguageModel: ChatLanguageModel,
        bookingTools: BookingTools,
        financialTools: FinancialTools,
        customerContentRetriever: ContentRetriever
    ): CustomerSupportAgent {
        return AiServices.builder(CustomerSupportAgent::class.java)
            .chatLanguageModel(chatLanguageModel)
            .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
            .tools(bookingTools, financialTools)
            .contentRetriever(customerContentRetriever)
            .build()
    }

    @Bean
    fun customerContentRetriever(
        customerEmbeddingStore: EmbeddingStore<TextSegment>,
        embeddingModel: EmbeddingModel
    ): ContentRetriever {
        // You will need to adjust these parameters to find the optimal setting, which will depend on two main factors:
        // - The nature of your data
        // - The embedding model you are using

        val maxResults = 1
        val minScore = 0.6

        return EmbeddingStoreContentRetriever.builder()
            .embeddingStore(customerEmbeddingStore)
            .embeddingModel(embeddingModel)
            .maxResults(maxResults)
            .minScore(minScore)
            .build()
    }

    @Bean
    fun embeddingModel(): EmbeddingModel {
        return AllMiniLmL6V2EmbeddingModel()
    }

    @Bean
    fun customerEmbeddingStore(embeddingModel: EmbeddingModel, resourceLoader: ResourceLoader): EmbeddingStore<TextSegment> {
        // Normally, you would already have your embedding store filled with your data.
        // However, for the purpose of this demonstration, we will:

        // 1. Create an in-memory embedding store

        val embeddingStore: EmbeddingStore<TextSegment> =  InMemoryEmbeddingStore()

        // 2. Load an example document ("Miles of Smiles" terms of use)
        val resource = resourceLoader.getResource("classpath:miles-of-smiles-terms-of-use.txt")
        val document = FileSystemDocumentLoader.loadDocument(resource.file.toPath(), TextDocumentParser())

        // 3. Split the document into segments 100 tokens each
        // 4. Convert segments into embeddings
        // 5. Store embeddings into embedding store
        // All this can be done manually, but we will use EmbeddingStoreIngestor to automate this:
        val documentSplitter = DocumentSplitters.recursive(100, 0, OpenAiTokenizer(OpenAiModelName.GPT_3_5_TURBO))
        val ingestor = EmbeddingStoreIngestor.builder()
            .documentSplitter(documentSplitter)
            .embeddingModel(embeddingModel)
            .embeddingStore(embeddingStore)
            .build()
        ingestor.ingest(document)

        return embeddingStore
    }

}

interface CustomerSupportAgent {
    @SystemMessage(
        "You are a customer support agent of a car rental company named 'Miles of Smiles'.",
        "Before providing information about booking or cancelling booking, you MUST always check:",
        "booking number, customer name and surname.",
        "Today is {{current_date}}."
    )
    fun chat(userMessage: String): String
}

fun main(args: Array<String>) {
    SpringApplication.run(CustomerSupportApplication::class.java, *args)
}

