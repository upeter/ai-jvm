package dev.example

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.ai.image.ImageOptions
import org.springframework.ai.image.ImagePrompt
import org.springframework.ai.image.ImageResponse
import org.springframework.ai.openai.OpenAiImageModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest


/**
 * - https://howtodoinjava.com/spring-ai/spring-ai-tutorial/
 * - https://github.com/lokeshgupta1981/spring-ai-examples/blob/master/openai-quickstart/src/main/java/com/howtodoinjava/ai/image/AppConfiguration.java
 */
@SpringBootTest
//@SpringBootTest(classes = OpenAiTestConfiguration::class)
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
class ImageGenTest @Autowired constructor(
    val imageOptions: ImageOptions,
    val imageModel: OpenAiImageModel
) {


    @Test
    fun `generate an image`() {
        val message = "Generate a comig style image of the cat Garfield that is being caught by a pet hunter net while eating up a huge lasagne"
        val imagePrompt: ImagePrompt = ImagePrompt(message, imageOptions)
        val response: ImageResponse = imageModel.call(imagePrompt)
        val imageUrl: String = response.result.output.url
        println(imageUrl)


    }



}


