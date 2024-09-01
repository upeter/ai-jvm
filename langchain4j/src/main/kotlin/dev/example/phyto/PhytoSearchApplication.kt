package dev.example.phyto

import dev.example.utils.CustomApachePdfBoxDocumentParser
import dev.example.utils.getText
import dev.langchain4j.data.document.Document
import dev.langchain4j.data.document.splitter.DocumentSplitters
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.memory.chat.MessageWindowChatMemory
import dev.langchain4j.model.chat.ChatLanguageModel
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
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ResourceLoader
import java.util.*
import java.nio.file.*
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser


@SpringBootApplication
class PhytoSearchApplication {
    /**
     * Run PhytoSearchApplicationTest to see simulated conversation with customer support agent
     */
    @Bean
    fun interactivePhytoChatRunner(agent: PhytoSupportAgent): ApplicationRunner {
        return ApplicationRunner { _: ApplicationArguments ->
            Scanner(System.`in`).use {scanner ->
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
    fun phytoAgent(
        chatLanguageModel: ChatLanguageModel,
        phytoContentRetriever: ContentRetriever
    ): PhytoSupportAgent {
        return AiServices.builder(PhytoSupportAgent::class.java)
            .chatLanguageModel(chatLanguageModel)
            .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
            .contentRetriever(phytoContentRetriever)
            .build()
    }


    @Bean
    fun phytoContentRetriever(
        pythoEmbeddingStore: EmbeddingStore<TextSegment>,
        embeddingModel: EmbeddingModel
    ): ContentRetriever {
        // You will need to adjust these parameters to find the optimal setting, which will depend on two main factors:
        // - The nature of your data
        // - The embedding model you are using

        val maxResults = 5
        val minScore = 0.6

        return EmbeddingStoreContentRetriever.builder()
            .embeddingStore(pythoEmbeddingStore)
            .embeddingModel(embeddingModel)
            .maxResults(maxResults)
            .minScore(minScore)
            .build()
    }


    @Bean
    fun pythoEmbeddingStore(
        @Value("\${pgvector.store.host}")  host:String,
        @Value("\${pgvector.store.port}")  port:Int,
        @Value("\${pgvector.store.user}")  user:String,
        @Value("\${pgvector.store.password}")  password:String,
        embeddingModel: EmbeddingModel, resourceLoader: ResourceLoader): EmbeddingStore<TextSegment> {
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
                .table("herbs")
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

        val documentSplitter = DocumentSplitters.recursive(100, 0, OpenAiTokenizer(OpenAiModelName.GPT_4))
        val ingestor = EmbeddingStoreIngestor.builder()
            .documentSplitter(documentSplitter)
            .embeddingModel(embeddingModel)
            .embeddingStore(embeddingStore)
            .build()
        Files.list(Paths.get("/Users/urs/development/github/ai/langchain4j/src/main/resources/pdfs")).toList()
            //.filter { it.fileName.endsWith(".pdf") }
            .map {
                println("Try ingesting...: ${it.fileName}")
                //val document = Document.from(getText(it.toFile(), 2,3,4,5).lines().takeWhile { !it.contains("""^REFERENCES$""".toRegex()) }.joinToString("\n"))
                val document = loadDocument(it, CustomApachePdfBoxDocumentParser(2,3,4,5,6,7, skipFromRegx = """^REFERENCES$""".toRegex()));
                ingestor.ingest(document);
                println("Ingested: ${it.fileName}")

        }

        return embeddingStore
    }

    @Bean
    fun embeddingModel(): EmbeddingModel {
        return AllMiniLmL6V2EmbeddingModel()
    }




}


interface PhytoSupportAgent {
    @SystemMessage(
        "You are a phytotherapy (herbal) healthcare advisor that advices herbal therapists to find herbal drugs for various disorders",
        "If you combine information that might not be related make sure that you notify the user that you did that. If you cannot provide an answer, you say: 'I don't have sufficient information to answer your question'",
        //"Crucial in your dialog is the disorder and herbal drugs to heal them or the herbal drug itself and it's healing qualities.",

    )
    fun chat(userMessage: String): String
}

fun main(args: Array<String>) {
    SpringApplication.run(PhytoSearchApplication::class.java, *args)
}

