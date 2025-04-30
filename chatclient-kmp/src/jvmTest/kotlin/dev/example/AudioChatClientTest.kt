package dev.example

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*

/**
 * Test for the audio chat client functionality
 * This test verifies that the client can successfully send audio data to the server
 * and receive a response.
 */
class AudioChatClientTest {

    @Test
    fun testAudioChatEndpoint() = runBlocking {
        // Create HTTP client
        val httpClient = HttpClient(CIO) {
            install(ContentNegotiation) {
                jackson()
            }
            // Add logging for debugging
            expectSuccess = false // Don't throw exceptions on non-2xx responses
        }

        try {
            try {
                // Get the sample.mp3 file from resources
                val classLoader = javaClass.classLoader
                val resourceUrl = classLoader.getResource("sample.mp3")
                println("Resource URL: $resourceUrl")

                if (resourceUrl == null) {
                    println("ERROR: Could not find sample.mp3 in resources")
                    println("Available resources:")
                    classLoader.getResources("").toList().forEach { url ->
                        println("- $url")
                        try {
                            File(url.file).listFiles()?.forEach { file ->
                                println("  - ${file.name}")
                            }
                        } catch (e: Exception) {
                            println("  Error listing files: ${e.message}")
                        }
                    }
                    throw IllegalStateException("Could not find sample.mp3 in resources")
                }

                val audioFile = File(resourceUrl.file)
                if (!audioFile.exists()) {
                    println("ERROR: Audio file does not exist at path: ${audioFile.absolutePath}")
                    throw IllegalStateException("Audio file does not exist")
                }

                println("Audio file size: ${audioFile.length()} bytes")
                println("Audio file path: ${audioFile.absolutePath}")

                // Generate a random conversation ID
                val conversationId = UUID.randomUUID().toString()

                // Send the request to the server
                println("Sending request to server...")
                try {
                    // Try using the simplest possible approach
                    val audioBytes = audioFile.readBytes()
                    println("Audio bytes length: ${audioBytes.size}")

                    // Use a different approach that mimics the successful test more closely
                    println("Sending request to server with audio bytes length: ${audioBytes.size}")
                    try {
                        // Try a different approach that more closely mimics the successful test
                        val response = httpClient.submitFormWithBinaryData(
                            url = "http://localhost:8080/ai/audio-chat",
                            formData = formData {
                                // Add the audio file part with explicit headers
                                append("audio", audioFile.readBytes(), Headers.build {
                                    append(HttpHeaders.ContentType, "audio/mpeg")
                                    append(HttpHeaders.ContentDisposition, "filename=\"${audioFile.name}\"")
                                })
                                // Add the conversation ID part
                                append("conversationId", conversationId)
                            }
                        ) {
                            // Set the Content-Type header for the entire request
                            header(HttpHeaders.ContentType, ContentType.MultiPart.FormData.toString())
                            // Set the Accept header to application/octet-stream
                            header(HttpHeaders.Accept, ContentType.Application.OctetStream.toString())
                        }

                        // Print response status
                        println("Response status: ${response.status}")

                        // Print response headers for debugging
                        println("Response headers:")
                        response.headers.forEach { name, values ->
                            println("  $name: $values")
                        }

                        // Print response body for debugging
                        try {
                            val responseText = response.bodyAsText()
                            println("Response body: $responseText")
                        } catch (e: Exception) {
                            println("Error reading response body: ${e.message}")
                            e.printStackTrace()
                        }

                        // Verify the response
                        assertEquals(200, response.status.value, "Response status should be 200 OK")

                        // Get response body
                        val responseBody = response.body<ByteArray>()

                        // Verify response body is not empty
                        assertTrue(responseBody.isNotEmpty(), "Response body should not be empty")
                        println("Response body size: ${responseBody.size} bytes")

                        println("Test completed successfully!")
                    } catch (e: Exception) {
                        println("ERROR during HTTP request: ${e.javaClass.name}: ${e.message}")
                        e.printStackTrace()
                        throw e
                    }

                } catch (e: Exception) {
                    println("ERROR during HTTP request: ${e.javaClass.name}: ${e.message}")
                    e.printStackTrace()
                    throw e
                }
            } catch (e: Exception) {
                println("ERROR in test: ${e.javaClass.name}: ${e.message}")
                e.printStackTrace()
                throw e
            }
        } finally {
            httpClient.close()
        }
    }
}
