package dev.example

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import kotlinx.coroutines.launch
import java.util.*

data class ChatMessage(
    val content: String,
    val isUserMessage: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatInput(
    val message: String,
    val conversationId: String = UUID.randomUUID().toString()
)

@Composable
@Preview
fun App() {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var conversationId by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var isLoading by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    val httpClient = remember {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                jackson()
            }
        }
    }

    MaterialTheme {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Hamburger menu
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu"
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Clear Conversation") },
                                onClick = {
                                    conversationId = UUID.randomUUID().toString()
                                    messages = listOf()
                                    menuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Progress bar
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                }

                // Chat messages area
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { message ->
                        ChatBubble(message)
                    }
                }

                // Input area
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message...") },
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (inputText.isNotBlank() && !isLoading) {
                                val userMessage = ChatMessage(inputText, true)
                                messages = messages + userMessage
                                isLoading = true

                                scope.launch {
                                    try {
                                        val response = httpClient.post("http://localhost:8080/ai/chat") {
                                            contentType(ContentType.Application.Json)
                                            setBody(ChatInput(inputText, conversationId))
                                        }

                                        val responseText = response.body<String>()
                                        messages = messages + ChatMessage(responseText, false)

                                        // Scroll to the bottom
                                        listState.animateScrollToItem(messages.size - 1)
                                    } catch (e: Exception) {
                                        messages = messages + ChatMessage("Error: ${e.message}", false)
                                    } finally {
                                        isLoading = false
                                        inputText = ""
                                    }
                                }
                            }
                        },
                        enabled = !isLoading && inputText.isNotBlank()
                    ) {
                        Text("Send")
                    }
                }
            }
        }
    }
}

sealed class ChatBubbleStyle {
    abstract val alignment: Alignment
    abstract val backgroundColor: Color
    abstract val textColor: Color

    object User : ChatBubbleStyle() {
        override val alignment = Alignment.CenterEnd
        override val backgroundColor = Color(0xFF2196F3)
        override val textColor = Color.White
    }

    object Agent : ChatBubbleStyle() {
        override val alignment = Alignment.CenterStart
        override val backgroundColor = Color(0xFFE0E0E0)
        override val textColor = Color.Black
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val style = if (message.isUserMessage) ChatBubbleStyle.User else ChatBubbleStyle.Agent
    ChatBubbleWithStyle(message.content, style)
}

@Composable
fun ChatBubbleWithStyle(content: String, style: ChatBubbleStyle) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = style.alignment
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (style is ChatBubbleStyle.Agent) Arrangement.Start else Arrangement.End
        ) {
            // Show agent icon only for agent messages
            if (style is ChatBubbleStyle.Agent) {
                Image(
                    painter = painterResource("AgentIcon.png"),
                    contentDescription = "Agent",
                    modifier = Modifier.size(40.dp).padding(end = 8.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = style.backgroundColor,
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                Text(
                    text = content,
                    modifier = Modifier.padding(12.dp),
                    color = style.textColor
                )
            }
        }
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "AI Chat Client",
        state = rememberWindowState(width = 800.dp, height = 600.dp)
    ) {
        App()
    }
}
