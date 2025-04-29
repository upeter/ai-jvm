package dev.example

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// No icons needed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*
import javax.sound.sampled.*
import javazoom.jl.player.Player

/**
 * Audio recorder class that handles recording audio using Java Sound API
 */
class AudioRecorder {
    private var audioFormat = AudioFormat(44100f, 16, 1, true, false)
    private var targetDataLine: TargetDataLine? = null
    private var recording = false
    private val byteArrayOutputStream = ByteArrayOutputStream()

    fun startRecording() {
        try {
            val info = DataLine.Info(TargetDataLine::class.java, audioFormat)
            targetDataLine = AudioSystem.getLine(info) as TargetDataLine
            targetDataLine?.open(audioFormat)
            targetDataLine?.start()

            recording = true

            // Start recording in a separate thread
            Thread {
                val data = ByteArray(targetDataLine!!.bufferSize / 5)
                while (recording) {
                    val count = targetDataLine!!.read(data, 0, data.size)
                    if (count > 0) {
                        byteArrayOutputStream.write(data, 0, count)
                    }
                }
                byteArrayOutputStream.close()
            }.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopRecording(): ByteArray {
        recording = false
        targetDataLine?.stop()
        targetDataLine?.close()

        // Convert to MP3 format using lame command-line tool
        return convertToMp3(byteArrayOutputStream.toByteArray())
    }

    private fun convertToMp3(pcmData: ByteArray): ByteArray {
        try {
            // Create temporary files for input and output
            val tempWavFile = File.createTempFile("recording", ".wav")
            val tempMp3File = File.createTempFile("recording", ".mp3")

            try {
                // Write WAV header and PCM data to temporary WAV file
                val wavOutputStream = FileOutputStream(tempWavFile)

                // Write WAV header
                val sampleRate = 44100
                val channels = 1
                val bitsPerSample = 16
                val dataSize = pcmData.size
                val format = 1 // PCM
                val blockAlign = channels * bitsPerSample / 8
                val byteRate = sampleRate * blockAlign

                // RIFF header
                wavOutputStream.write("RIFF".toByteArray())
                wavOutputStream.write(intToByteArray(36 + dataSize)) // File size - 8
                wavOutputStream.write("WAVE".toByteArray())

                // fmt chunk
                wavOutputStream.write("fmt ".toByteArray())
                wavOutputStream.write(intToByteArray(16)) // Chunk size
                wavOutputStream.write(shortToByteArray(format.toShort())) // Format
                wavOutputStream.write(shortToByteArray(channels.toShort())) // Channels
                wavOutputStream.write(intToByteArray(sampleRate)) // Sample rate
                wavOutputStream.write(intToByteArray(byteRate)) // Byte rate
                wavOutputStream.write(shortToByteArray(blockAlign.toShort())) // Block align
                wavOutputStream.write(shortToByteArray(bitsPerSample.toShort())) // Bits per sample

                // data chunk
                wavOutputStream.write("data".toByteArray())
                wavOutputStream.write(intToByteArray(dataSize)) // Chunk size
                wavOutputStream.write(pcmData) // Audio data
                wavOutputStream.close()

                // Convert WAV to MP3 using lame
                val process = ProcessBuilder(
                    "lame", 
                    "--preset", "standard", 
                    tempWavFile.absolutePath, 
                    tempMp3File.absolutePath
                ).start()

                // Wait for the process to complete
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    println("Error converting to MP3: lame exited with code $exitCode")
                    val errorOutput = process.errorStream.bufferedReader().readText()
                    println("Error output: $errorOutput")
                    return pcmData // Return original data if conversion fails
                }

                // Read the MP3 file
                val mp3Data = tempMp3File.readBytes()
                return mp3Data
            } finally {
                // Clean up temporary files
                tempWavFile.delete()
                tempMp3File.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return pcmData // Return original data if conversion fails
        }
    }

    private fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            value.toByte(),
            (value shr 8).toByte(),
            (value shr 16).toByte(),
            (value shr 24).toByte()
        )
    }

    private fun shortToByteArray(value: Short): ByteArray {
        return byteArrayOf(
            value.toByte(),
            (value.toInt() shr 8).toByte()
        )
    }

}

/**
 * Audio player class that handles playing audio using JLayer MP3 library
 */
class AudioPlayer {
    private var player: Player? = null
    private var playerThread: Thread? = null

    fun playAudio(audioData: ByteArray, onComplete: () -> Unit) {
        try {
            // Stop any existing playback
            stop()

            // Create a new player with the audio data
            val inputStream = ByteArrayInputStream(audioData)
            player = Player(inputStream)

            // Play in a separate thread
            playerThread = Thread {
                try {
                    player?.play()
                    // When playback is complete, call onComplete
                    onComplete()
                } catch (e: Exception) {
                    e.printStackTrace()
                    onComplete()
                }
            }
            playerThread?.start()
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete()
        }
    }

    fun stop() {
        player?.close()
        playerThread?.interrupt()
        player = null
        playerThread = null
    }
}

/**
 * Composable function for the audio chat screen
 */
@Composable
fun AudioChatScreen(httpClient: HttpClient) {
    val scope = rememberCoroutineScope()
    var isRecording by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }

    // Animation for the record button
    val animatedSize by animateFloatAsState(
        targetValue = if (isRecording) 1.2f else 1f,
        animationSpec = tween(durationMillis = 300)
    )

    // Animation for the agent icon
    val agentScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "agentScale"
    )

    // Create audio recorder and player
    val audioRecorder = remember { AudioRecorder() }
    val audioPlayer = remember { AudioPlayer() }

    // Conversation ID for the chat
    val conversationId = remember { UUID.randomUUID().toString() }

    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Agent icon (only visible when playing)
            if (isPlaying) {
                Image(
                    painter = painterResource("AgentIcon.png"),
                    contentDescription = "Agent",
                    modifier = Modifier
                        .size(120.dp)
                        .scale(agentScale),
                    alignment = Alignment.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Record button
            Box(
                modifier = Modifier
                    .size(100.dp * animatedSize)
                    .clip(CircleShape)
                    .background(if (isRecording) Color.Red else MaterialTheme.colorScheme.primary)
                    .border(2.dp, Color.Gray, CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (!isProcessing) {
                            if (isRecording) {
                                // Stop recording and send audio
                                isRecording = false
                                isProcessing = true

                                scope.launch {
                                    try {
                                        // Stop recording and get audio data
                                        val audioData = audioRecorder.stopRecording()

                                        // Save to temporary file (for multipart form data)
                                        val tempFile = withContext(Dispatchers.IO) {
                                            val file = File.createTempFile("audio", ".mp3")
                                            file.writeBytes(audioData)
                                            file
                                        }

                                        // Send to server
                                        val response = httpClient.submitFormWithBinaryData(
                                            url = "http://localhost:8080/ai/audio-chat",
                                            formData = formData {
                                                append("audio", tempFile.readBytes(), Headers.build {
                                                    append(HttpHeaders.ContentType, "audio/mp3")
                                                    append(HttpHeaders.ContentDisposition, "filename=audio.mp3")
                                                })
                                                append("conversationId", conversationId)
                                            },

                                        )

                                        // Get response audio
                                        val responseAudio = response.body<ByteArray>()

                                        // Play response
                                        isPlaying = true
                                        audioPlayer.playAudio(responseAudio) {
                                            isPlaying = false
                                        }

                                        // Clean up temp file
                                        withContext(Dispatchers.IO) {
                                            tempFile.delete()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        isProcessing = false
                                    }
                                }
                            } else {
                                // Start recording
                                isRecording = true
                                audioRecorder.startRecording()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "REC",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            // Status text
            Text(
                text = when {
                    isRecording -> "Recording... (Release to send)"
                    isProcessing -> "Processing..."
                    isPlaying -> "Playing response..."
                    else -> "Tap and hold to record"
                },
                style = MaterialTheme.typography.bodyLarge
            )

            // Progress indicator for processing
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
