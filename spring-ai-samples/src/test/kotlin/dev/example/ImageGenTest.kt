package dev.example

import javazoom.jl.player.Player
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.ai.image.ImageResponse
import org.springframework.ai.openai.OpenAiAudioSpeechModel
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel
import org.springframework.ai.openai.audio.speech.SpeechPrompt
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.Resource
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.InputStream
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt
import javax.sound.sampled.LineEvent
import javax.sound.sampled.LineListener


/**
 * - https://github.com/spring-projects/spring-ai/blob/main/models/spring-ai-openai/src/test/java/org/springframework/ai/openai/audio/transcription/OpenAiTranscriptionModelIT.java
 * - https://howtodoinjava.com/spring-ai/transcription-speech-to-text/
 * - https://www.rev.com/onlinevoicerecorder
 */
@SpringBootTest
//@SpringBootTest(classes = OpenAiTestConfiguration::class)
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
class TextToSpeechTest @Autowired constructor(
    @Value("classpath:sample.mp3")
    val audioFile: Resource,
    val transcriptionModel: OpenAiAudioTranscriptionModel,
    val speechModel: OpenAiAudioSpeechModel,
) {

//    @Test
//    fun shouldGenerateNonEmptyMp3AudioFromSpeechPrompt() {
//        val speechOptions: OpenAiAudioSpeechOptions = OpenAiAudioSpeechOptions.builder()
//            .withVoice(OpenAiAudioApi.SpeechRequest.Voice.ALLOY)
//            .withSpeed(SPEED)
//            .withResponseFormat(OpenAiAudioApi.SpeechRequest.AudioResponseFormat.MP3)
//            .withModel(OpenAiAudioApi.TtsModel.TTS_1.value)
//            .build()
//        val speechPrompt: SpeechPrompt = SpeechPrompt(
//            "Today is a wonderful day to build something people love!",
//            speechOptions
//        )
//        val response: SpeechResponse = speechModel.call(speechPrompt)
//        val audioBytes: ByteArray = response.getResult().getOutput()
//        assertThat(response.getResults()).hasSize(1)
//        assertThat(response.getResults().get(0).getOutput()).isNotEmpty()
//        assertThat(audioBytes).hasSizeGreaterThan(0)
//    }

    @Test
    fun `transcribe mp3`() {
        //playMp3(audioFile.inputStream)
        val response = transcriptionModel.call(AudioTranscriptionPrompt(audioFile))
        val text = response.result.output
        println(text)
    }


    @Test
    fun `text to mp3`() {
        val message = "The AI world is awesome, don't you think so?"
        val speechResponse = speechModel.call(SpeechPrompt(message))
        val audio = speechResponse.result.output
        playMp3(ByteArrayInputStream(audio))

    }

}

fun playMp3(inputStream:InputStream) {
    try {
        val buffer = BufferedInputStream(inputStream)
        val mp3Player = Player(buffer)
        mp3Player.play()
    } catch (ex: Exception) {
        println("Error occured during playback process:" + ex.message)
    }
}

