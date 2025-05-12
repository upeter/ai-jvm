import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.GetPromptResult
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.PromptArgument
import io.modelcontextprotocol.kotlin.sdk.PromptMessage
import io.modelcontextprotocol.kotlin.sdk.ReadResourceResult
import io.modelcontextprotocol.kotlin.sdk.Role
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.TextResourceContents
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import kotlinx.serialization.Serializable
import io.ktor.utils.io.streams.*
import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.buffered
import io.ktor.serialization.kotlinx.json.*
import kotlinx.io.asSource
import kotlinx.serialization.json.*
import kotlin.text.append


fun configureMCPServer(): Server {
    val server = Server(
        Implementation(
            name = "mcp-server italian delaight",
            version = "0.1.0"
        ),
        ServerOptions(
            capabilities = ServerCapabilities(
                prompts = ServerCapabilities.Prompts(listChanged = true),
                resources = ServerCapabilities.Resources(subscribe = true, listChanged = false),
                tools = ServerCapabilities.Tools(listChanged = true),
            )
        )
    )


    // Add a resource
    server.addResource(
        uri = "file:///italian/delaight/menu.md",
        name = "complete-menu-italian-delaight-restaurant",
        description = "The complete menu and dishes of the Italian DelAIght restaurant",
        mimeType = "text/markdown"
    ) { request ->
        ReadResourceResult(
            contents = listOf(
                TextResourceContents(
                    text = MCPServices::class.java.getResourceAsStream("/menu.md").reader().readText(),
                    uri = "unknown",
                    mimeType = "text/markdown"
                )
            )
        )
    }

    // Add a prompt
    server.addPrompt(
        name = "italian-meal-agent",
        description = "suggests dishes and handles orders using a conversational waiter AI of the Italian DelAIght restaurant",
        arguments = emptyList()
    ) { request ->
        GetPromptResult(
            description = null,
            messages = listOf(
                PromptMessage(
                    role = Role.user,
                    content = TextContent(ITALIAN_AGENT_PROMPT)
                )
            )
        )
    }

    // Add a tools
    server.addTool(
        name = "classify-prompt-if-food-or-other",
        description = "Classifies a prompt to verify whether it is food or something else. If classified as food, extracted food items are returned.",
    ) { request ->
        CallToolResult(
            content = listOf(
                TextContent(
                    """
        |Classify the following prompt as 'food' or 'other'. If it is about food, extract dish name and/or ingredients. 
        |The prompt is=[${request.arguments}]
        |Your response should be in JSON format.
        |Do not include any explanations, only provide a RFC8259 compliant JSON response following this format without deviation.
        |Do not include markdown code blocks in your response.
        |Remove the ```json markdown from the output.
        |Here is the JSON Schema instance your output must adhere to:
        |```{
        |   "${'$'}schema": "https://json-schema.org/draft/2020-12/schema",
        |   "type": "object",
        |   "properties": {
        |       "classification": {
        |           "type": "string",
        |           "enum": ["FOOD", "OTHER"]
        |        },
        |       "foodElements": {
        |           "type": "array",
        |           "items": {
        |                "type": "string"
        |           }
        |       }
        |   },
        |   "additionalProperties": false
        |}```
        |""".trimMargin()
                )
            )
        )
    }


    server.addTool(
        name = "find-dishes-service",
        description = """Select matching dishes for given food elements like meal or dish name and or ingredients.""".trimIndent(),
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                put("foodElements", buildJsonObject {
                    put("type", JsonPrimitive("array"))
                    put("items", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                    })
                })
            },
            required = listOf("foodElements"),
        )
    ) { request ->
        val dishSelectionRequest =
            request.arguments.get("foodElements")?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty()

        val selectedDishes = selectDishes(dishSelectionRequest)

        CallToolResult(
            content = selectedDishes.map { TextContent(it) }
        )
    }


    server.addTool(
        name = "order-dish-service",
        description = """Dish order service for the italian delaight restaurant.""".trimIndent(),
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                put("meals", buildJsonObject {
                    put("type", JsonPrimitive("array"))
                    put("items", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                    })
                })
            },
            required = listOf("meals"),
        )
    ) { request ->
        val orderRequest = Json.decodeFromJsonElement<OrderRequest>(request.arguments)

        val orderResponse = orderDish(orderRequest)

        CallToolResult(
            content = listOf(TextContent("Order placed! Dishes will be delivered in ${orderResponse.deliveredInMinutes} minutes."))
        )
    }





    return server
}

val ITALIAN_AGENT_PROMPT = """ You are an Italian waiter AI who assists customers in choosing and ordering dishes.
Here's how to behave:
- If the user wants to have some ideas about dishes run `complete-menu-italian-delaight-restaurant` to have the complete menu.
- If the user gives food preferences or ingredients, use the `find-dishes-service` tool to find matching dishes.
- before looking for preferred meals first run the `classify-prompt-if-food-or-other` tool to understand whether the prompt is about a food preference or not.
- Propose ALL matching dishes from the `find-dishes-service` result.
- If the customer confirms a dish, call the `order-dish-service` tool.
- Once the order is placed, thank them and summarize the dish names with the estimated delivery time.

Only use the tools to gather or act on information. Do not invent dishes. Be polite, helpful, and speak in a friendly English tone.
""".trimIndent()


@Serializable
data class OrderRequest(val meals: List<String>)

@Serializable
data class OrderResponse(val deliveredInMinutes: Int)

fun runMcpServerUsingStdio() {
    // Note: The server will handle listing prompts, tools, and resources automatically.
    // The handleListResourceTemplates will return empty as defined in the Server code.
    val server = configureMCPServer()
    val transport = StdioServerTransport(
        inputStream = System.`in`.asSource().buffered(),
        outputStream = System.out.asSink().buffered()
    )

    runBlocking {
        server.connect(transport)
        //println("Server connected")
        val done = Job()
        server.onClose {
            done.complete()
        }
        done.join()
        println("Server closed")
    }
}

fun main(args: Array<String>) {
    runMcpServerUsingStdio()
}

class MCPServices {}

// Base URL for the API
val baseUrl = "http://localhost:8080"

// Create an HTTP client with a default request configuration and JSON content negotiation
val httpClient = HttpClient {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        })
    }
}

suspend fun orderDish(order: OrderRequest): OrderResponse {
    val response: HttpResponse = httpClient.post("$baseUrl/ai/order-dish") {
        contentType(ContentType.Application.Json)
        accept(ContentType.Application.Json)
        setBody(order)
    }
    return response.body()
}


suspend fun selectDishes(foodElements: List<String>): List<String> {
    val response: HttpResponse = httpClient.get("$baseUrl/ai/find-dishes") {
        url {
            parameters.append("foodElements", foodElements.joinToString(","))
        }
        contentType(ContentType.Application.Json)
        accept(ContentType.Application.Json)
    }
    return response.body()
}

//{ "jsonrpc": "2.0", "method": "resources/read","params": {  "uri" : "file:///italian/delaight/menu.md", "resource": "complete-menu-italian-delaight-restaurant" },"id": 1 }
//{ "jsonrpc": "2.0", "method": "resources/get","params": {  "uri" : "file:///italian/delaight/menu.md", "resource": "complete-menu-italian-delaight-restaurant" },"id": 1 }
//
