package dev.example

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import java.util.*

// Enum to represent different screens in the app
enum class Screen {
    TEXT_CHAT,
    AUDIO_CHAT
}

@Composable
@Preview
fun App() {
    var conversationId by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var menuExpanded by remember { mutableStateOf(false) }
    var currentScreen by remember { mutableStateOf(Screen.TEXT_CHAT) }

    val httpClient = remember {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                jackson()
            }
        }
    }

    MaterialTheme {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp).background(Color.White)
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
                                text = { Text("Text Chat") },
                                onClick = {
                                    currentScreen = Screen.TEXT_CHAT
                                    menuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Audio Chat") },
                                onClick = {
                                    currentScreen = Screen.AUDIO_CHAT
                                    menuExpanded = false
                                }
                            )
                            Divider()
                            DropdownMenuItem(
                                text = { Text("Clear Conversation") },
                                onClick = {
                                    conversationId = UUID.randomUUID().toString()
                                    menuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Screen content based on current screen
                when (currentScreen) {
                    Screen.TEXT_CHAT -> {
                        // Display the text chat screen
                        TextChatScreen(httpClient, conversationId)
                    }

                    Screen.AUDIO_CHAT -> {
                        // Display the audio chat screen
                        AudioChatScreen(httpClient, conversationId)
                    }
                }
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
