package dev.example.demo

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.logging.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import java.net.URI

/**
 * Performs a GET request to the specified URL.
 * If the URL contains "/ask/stream", it will handle the response as a streaming response.
 */
fun GET(url: String): String = runBlocking {
    val client = HttpClient(CIO) {
        install(Logging) {
            level = LogLevel.INFO
        }
    }

    try {
        if (url.contains("/ask/stream")) {
            // Handle streaming response
            val flow = getStreamingResponse(client, url)
            flow.collect {
                println(it)
            }
            ""
        } else {
            // Handle regular response
            val response = client.get(URI(url.replace(" ", "%20")).toString())
            response.bodyAsText()
        }
    } finally {
        client.close()
    }
}

/**
 * Gets a streaming response from the specified URL.
 * This implementation properly handles streaming by reading from the response channel.
 */
private suspend fun getStreamingResponse(client: HttpClient, url: String): Flow<String> = flow {
    val response = client.get(URI(url.replace(" ", "%20")).toString())
    val channel = response.bodyAsChannel()

    val buffer = ByteArray(1024)
    var bytesRead: Int
    var remainingText = ""

    while (!channel.isClosedForRead) {
        bytesRead = channel.readAvailable(buffer, 0, buffer.size)
        if (bytesRead < 0) break

        val text = buffer.decodeToString(0, bytesRead)
        val fullText = remainingText + text

        // Process the text by lines or chunks as appropriate
        // This approach handles cases where a chunk might be split across reads
        val lines = fullText.split("\n")

        // Emit all complete lines
        for (i in 0 until lines.size - 1) {
            val line = lines[i].trim()
            if (line.isNotEmpty()) {
                emit(line)
            }
        }

        // Keep the last potentially incomplete line for the next iteration
        remainingText = lines.last()
    }

    // Emit any remaining text
    if (remainingText.isNotEmpty()) {
        emit(remainingText)
    }
}
