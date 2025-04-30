package dev.example

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.io.Resource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import java.io.ByteArrayInputStream
import java.io.File
import org.assertj.core.api.Assertions.assertThat
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpMethod

/**
 * Test for the /ai/audio-chat endpoint
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
class AudioChatTest @Autowired constructor(
    private val restTemplate: TestRestTemplate,
    @Value("classpath:recording17357203580683092034.mp3")
    private val audioFile: Resource
) {

    @Test
    fun `audio chat endpoint should process audio and return audio response`() {
        // Prepare the multipart request
        val headers = HttpHeaders()
        headers.contentType = MediaType.MULTIPART_FORM_DATA
        
        val body: MultiValueMap<String, Any> = LinkedMultiValueMap()
        body.add("audio", audioFile.file.toMultipartFile())
        body.add("conversationId", "test-conversation-id")
        
        val requestEntity = HttpEntity(body, headers)
        
        // Call the endpoint
        val response = restTemplate.exchange(
            "/ai/audio-chat",
            HttpMethod.POST,
            requestEntity,
            ByteArray::class.java
        )
        
        // Verify the response
        assertThat(response.statusCode.is2xxSuccessful).isTrue()
        assertThat(response.body).isNotNull
        assertThat(response.body!!.size).isGreaterThan(0)
        
        // Optionally play the audio (commented out for automated testing)
        playMp3(ByteArrayInputStream(response.body))
        
        println("Audio response received with size: ${response.body!!.size} bytes")
    }
}

/**
 * Extension function to convert a File to a MultipartFile
 */
fun File.toMultipartFile(): NamedByteArrayResource {
    return NamedByteArrayResource(
        this.readBytes(),
        this.name
    )
}

class NamedByteArrayResource(
    private val bytes: ByteArray,
    private val filename: String
) : ByteArrayResource(bytes) {
    override fun getFilename(): String = filename
}