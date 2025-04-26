import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonObject

class MCPServices {}
fun configureMCPServer(): Server {
    val server = Server(
        Implementation(
            name = "mcp-kotlin test server",
            version = "0.1.0"
        ),
        ServerOptions(
            capabilities = ServerCapabilities(
                prompts = ServerCapabilities.Prompts(listChanged = true),
                resources = ServerCapabilities.Resources(subscribe = true, listChanged = true),
                tools = ServerCapabilities.Tools(listChanged = true),
            )
        )
    )
    // Base URL for the Weather API
    val baseUrl = "http://localhost:8080"

    // Create an HTTP client with a default request configuration and JSON content negotiation
    val httpClient = HttpClient {
//        defaultRequest {
//            url(baseUrl)
//            headers {
//                append("Accept", "application/geo+json")
//                append("User-Agent", "WeatherApiClient/1.0")
//            }
//            contentType(ContentType.Application.Json)
//        }
        // Install content negotiation plugin for JSON serialization/deserialization
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
    }

    server.addPrompt(
        name = "classify-prompt-if-food-or-other",
        description = "Classifies a prompt to verify whether it is food or something else. If classified as food, extracted food items are returned.",
        arguments = listOf(
            PromptArgument(
                name = "Prompt",
                description = "The user prompt",
                required = true
            )
        )
    ) { request ->
        GetPromptResult(description = null,
            messages = listOf(
                PromptMessage(
                    role = Role.user,
                    content = TextContent("""
        |Classify the following prompt as 'food' or 'other'. If it is about food, extract dish name and/or ingredients. 
        |The prompt is=[${request.arguments?.get("Prompt")}]
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
        |""".trimMargin()))))
    }

    // Add a tool
    server.addTool(
        name = "kotlin-sdk-tool",
        description = "A test tool",
        inputSchema = Tool.Input()
    ) { request ->
        CallToolResult(
            content = listOf(TextContent("Hello, world!"))
        )
    }

    // Add a resource
    server.addResource(
        uri = "file:///italian/delaight/menu.md",
        name = "complete-menu-italian-delaight-restaurant",
        description = "The complete menu of the Italian DelAIght restaurant",
        mimeType = "text/markdown"
    ) { request ->
        ReadResourceResult(
            contents = listOf(
                TextResourceContents(text = MCPServices::class.java.getResourceAsStream("/menu.md").reader().readText(), uri = "unknown", mimeType =  "text/markdown")
            )
        )
    }


//    @Tool(name = "order-dish-service", description = "Dish order service")
//    fun orderDish(@ToolParam(description = "Dishes that will be ordered") order: dev.example.OrderRequest): dev.example.OrderResponse {
//        return restClient.post()
//            .uri("/ai/order-dish")
//            .contentType(MediaType.APPLICATION_JSON)
//            .body(order)
//            .accept(MediaType.APPLICATION_JSON)
//            .retrieve()
//            .requiredBody()
//    }
    server.addTool(
        name = "order-dish-service",
        description = """Dish order service for the italian delaight restaurant.""".trimIndent(),
        inputSchema = Tool.Input(
            properties =  buildJsonObject {
                putJsonObject("meals") {
                    put("type", "array")
                    put("description", "Dishes that will be ordered")
                    putJsonObject("items") {
                        put("type", "string")
                    }
                }
            },
            required = listOf("meals"),)
    ) { request ->
        val orderRequest = Json.decodeFromJsonElement<OrderRequest>(request.arguments)

        val orderResponse = orderDish(orderRequest)

        CallToolResult(
            content = listOf(TextContent("Order placed! Dishes will be delivered in ${orderResponse.deliveredInMinutes} minutes."))
        )
    }

    suspend fun orderDish(order: OrderRequest): OrderResponse {
        val response: HttpResponse = httpClient.post("$baseUrl/ai/order-dish") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(order)
        }
        return response.body()
    }



    return server
}

@Serializable
data class OrderRequest(val meals: List<String>)

@Serializable
data class OrderResponse(val deliveredInMinutes: Int)

