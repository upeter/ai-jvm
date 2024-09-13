package dev.example

import dev.example.ColorPrinter.Color.BLUE
import dev.example.ColorPrinter.Color.CYAN
import dev.example.ColorPrinter.cprint
import dev.example.ColorPrinter.cprintln
import dev.example.ReplyType.AUDIO
import dev.example.ReplyType.TEXT
import kotlinx.coroutines.flow.Flow
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.bodyToFlow
import reactor.netty.resources.ConnectionProvider
import java.io.ByteArrayInputStream
import java.util.*
import java.time.*
import org.springframework.http.client.reactive.*
import reactor.netty.http.client.HttpClient


suspend fun main() {
    var communicationProfile = CommunicationProfile()
    val webClient = WebClient.builder().exchangeStrategies(exchangeStrategies).clientConnector(provider).build()
    Scanner(System.`in`).use { scanner ->
        while (true) {
            cprint(BLUE, "User: ")
            val userInput = scanner.nextLine()
            if (userInput.isNotBlank()) {
                when (userInput) {
                    "restart" -> communicationProfile = communicationProfile.copy(conversationId = createConversionId()).also{
                        cprintln(CYAN, "> Restarted conversation with id ${communicationProfile.conversationId}")
                    }
                    "audio" -> communicationProfile = communicationProfile.copy(replyType = AUDIO).also{
                        cprintln(CYAN, "> Set to audio chat")
                    }
                    "text" -> communicationProfile = communicationProfile.copy(replyType = TEXT).also{
                        cprintln(CYAN, "> Set to text chat")
                    }
                    "exit" -> {
                        cprintln(CYAN, "> Exiting Bye Bye")
                        break
                    }

                    else -> when (communicationProfile.replyType) {
                        TEXT -> webClient.handleTextChat(userInput, communicationProfile.conversationId)
                        AUDIO -> webClient.handleAudioChat(userInput, communicationProfile.conversationId)
                    }
                }
            }
        }
    }


}


val exchangeStrategies = ExchangeStrategies.builder()
    .codecs { configurer ->
        configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) // 10 MB

    }

    .build()

val provider = ReactorClientHttpConnector(
    HttpClient.create(ConnectionProvider.builder("fixed")
.maxConnections(500)
.maxIdleTime(Duration.ofSeconds(20))
.maxLifeTime(Duration.ofSeconds(60))
.pendingAcquireTimeout(Duration.ofSeconds(60))
.evictInBackground(Duration.ofSeconds(120)).build()))


fun WebClient.streamingChat(chatRequest: ChatInput): Flow<String> {
    val url = "http://localhost:8080/ai/chat"
    return this.post()
        .uri(url)
        .bodyValue(chatRequest)
        .retrieve()
        .bodyToFlow<String>()
}

suspend fun WebClient.audioChat(chatRequest: ChatInput): ByteArray {
    val url = "http://localhost:8080/ai/speech"
    return this.post()
        .uri(url)
        .bodyValue(chatRequest)
        .retrieve()
        .awaitBody<ByteArray>()
}

enum class ReplyType {
    TEXT, AUDIO
}


fun createConversionId(): String = System.currentTimeMillis().toString()
data class CommunicationProfile(
    val conversationId: String = System.currentTimeMillis().toString(),
    val replyType: ReplyType = TEXT,
)

suspend fun WebClient.handleAudioChat(userMessage: String, conversationId: String) {
    println()
    cprintln(ColorPrinter.Color.RED, "Agent: 🔈")
    this.audioChat(ChatInput(userMessage, conversationId, latitude = "-70.432", longitude = "40.8")).also {
        playMp3(ByteArrayInputStream(it))
    }
    println()
}


suspend fun WebClient.handleTextChat(userMessage: String, conversationId: String) {
    println()
    cprintln(ColorPrinter.Color.RED, "Agent:")
    this.streamingChat(ChatInput(userMessage, conversationId, latitude = "-70.432", longitude = "40.8")).collect {
        cprintln(ColorPrinter.Color.BLACK, it)
    }
    println()
}


object ColorPrinter {
    fun cprint(col: Color, msg: String) {
        val colStr = when (col) {
            Color.BLACK -> "\u001B[30m"
            Color.RED -> "\u001B[31m"
            Color.GREEN -> "\u001B[32m"
            Color.YELLOW -> "\u001B[33m"
            BLUE -> "\u001B[34m"
            Color.PURPLE -> "\u001B[35m"
            CYAN -> "\u001B[36m"
            Color.WHITE -> "\u001B[37m"
        }

        print("$colStr$msg\u001B[0m")
    }

    fun cprintln(col: Color, msg: String) {
        cprint(col, msg)
        println()
    }

    enum class Color {
        BLACK, RED, GREEN, YELLOW, BLUE, PURPLE, CYAN, WHITE
    }
}