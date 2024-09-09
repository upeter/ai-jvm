package dev.example

import kotlinx.coroutines.flow.Flow
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlow
import java.util.*

val exchangeStrategies = ExchangeStrategies.builder()
    .codecs { configurer ->
        configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) // 10 MB
    }
    .build()


fun WebClient.streamingChat(chatRequest: ChatInput): Flow<String> {
    val url = "http://localhost:8080/ai/chat"
    return this.post()
        .uri(url)
        .bodyValue(chatRequest)
        .retrieve()
        .bodyToFlow<String>()
}

suspend fun main() {

    val conversationId = System.currentTimeMillis().toString()

    val webClient = WebClient.builder().exchangeStrategies(exchangeStrategies).build()

    Scanner(System.`in`).use { scanner ->
        while (true) {
            ColorPrinter.print(ColorPrinter.Color.BLUE,"User: ")
            val userMessage = scanner.nextLine()
            if ("exit".equals(userMessage, ignoreCase = true)) {
                break
            }
            ColorPrinter.println(ColorPrinter.Color.RED,"Agent:")
            webClient.streamingChat(ChatInput(userMessage, conversationId, latitude = "-70.432", longitude = "40.8")).collect{
                ColorPrinter.println(ColorPrinter.Color.BLACK, it)
            }

        }
    }
}

object ColorPrinter {
    fun print(col: Color, msg: String) {
        val colStr = when (col) {
            Color.BLACK -> "\u001B[30m"
            Color.RED -> "\u001B[31m"
            Color.GREEN -> "\u001B[32m"
            Color.YELLOW -> "\u001B[33m"
            Color.BLUE -> "\u001B[34m"
            Color.PURPLE -> "\u001B[35m"
            Color.CYAN -> "\u001B[36m"
            Color.WHITE -> "\u001B[37m"
        }

        print("$colStr$msg\u001B[0m")
    }

    fun println(col: Color, msg: String) {
        print(col, msg)
        println()
    }

    enum class Color {
        BLACK, RED, GREEN, YELLOW, BLUE, PURPLE, CYAN, WHITE
    }
}