package dev.example

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.launch

data class ChatMessage(
    val content: String,
    val isUserMessage: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatInput(
    val message: String,
    val conversationId: String
)

sealed class ChatBubbleStyle {
    abstract val alignment: Alignment
    abstract val backgroundColor: Color
    abstract val textColor: Color

    object User : ChatBubbleStyle() {
        override val alignment = Alignment.CenterEnd
        override val backgroundColor = Color(0xFF009246) // Italian green
        override val textColor = Color.White
    }

    object Agent : ChatBubbleStyle() {
        override val alignment = Alignment.CenterStart
        override val backgroundColor = Color(0xFFCE2B37) // Italian red
        override val textColor = Color.White
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp),
        contentAlignment = style.alignment
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = if (style is ChatBubbleStyle.Agent) Arrangement.Start else Arrangement.End
        ) {
            // Show agent icon only for agent messages
            if (style is ChatBubbleStyle.Agent) {
                Image(
                    painter = painterResource("AgentIcon.png"),
                    contentDescription = "Agent",
                    modifier = Modifier.size(60.dp).padding(end = 8.dp),
                    alignment = Alignment.TopStart
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = style.backgroundColor,
                modifier = Modifier.widthIn(max = 400.dp)
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

@Composable
fun TextChatScreen(httpClient: HttpClient, conversationId: String) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    var inputText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var isLoading by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxSize()) {
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
            // Create focus requesters for input field and send button
            val inputFieldFocus = remember { FocusRequester() }
            val sendButtonFocus = remember { FocusRequester() }

            // Function to send message
            val sendMessage = {
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
                            // Return focus to input field after sending
                            inputFieldFocus.requestFocus()
                        }
                    }
                }
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp, bottom = 8.dp)
                    .focusRequester(inputFieldFocus)
                    .focusProperties {
                        next = sendButtonFocus
                    }
                    .onKeyEvent { event ->
                        when (event.key) {
                            Key.Tab -> {
                                // Move focus to send button when Tab is pressed
                                sendButtonFocus.requestFocus()
                                true // Consume the event to prevent default behavior
                            }
                            Key.Enter -> {
                                // Send message when Enter is pressed
                                sendMessage()
                                true // Consume the event
                            }
                            else -> false // Don't consume other key events
                        }
                    },
                placeholder = { Text("Type a message...") },
                maxLines = 3
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { sendMessage() },
                modifier = Modifier
                    .padding(end = 8.dp, bottom = 8.dp)
                    .focusRequester(sendButtonFocus)
                    .focusProperties {
                        previous = inputFieldFocus
                    },
                enabled = !isLoading && inputText.isNotBlank()
            ) {
                Text("Send")
            }
        }
    }
}