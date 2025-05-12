package dev.example

import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.ToolCallbacks
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service


import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import org.springframework.web.client.requiredBody

@Service
class FileService {

    @Tool(name = "list-files-in-directory", description = "List files in directory")
    fun listFilesInDirectory(@ToolParam(description = "directory") directory: String, @ToolParam(description = "extension", required = false) extension:String?): List<String> {
        val dir = java.io.File("$ROOT_DIR/$directory")
        return if (dir.exists() && dir.isDirectory) {
            dir.listFiles().filter {file -> extension?.let{file.name.lowercase().endsWith(it.lowercase())} != false }
                .map { it.name }
        } else {
            emptyList()
        }
    }

    companion object {
        const val ROOT_DIR = "/Users/urs/"
    }
}

@Service
class FoodService(@Value("\${food-server-url}") private val baseUrl: String,) {

    private val restClient = RestClient.builder()
        .baseUrl(baseUrl)
        .build()


//    @Tool(
//        name = "classify-prompt-if-food-or-other",
//        description = "Classifies a prompt to verify whether it is food or something else. If classified as food, extracted food items are returned."
//    )
//    fun classifyPrompt(@ToolParam(description = "prompt") prompt: String): PromptClassification? {
//        return restClient.get()
//            .uri { uriBuilder ->
//                uriBuilder.path("/ai/prompt-classifier")
//                    .queryParam("prompt", prompt)
//                    .build()
//            }
//            .accept(MediaType.APPLICATION_JSON)
//            .retrieve()
//            .requiredBody()
//    }

    @Tool(
        name = "classify-prompt-if-food-or-other",
        description = "Classifies a prompt to verify whether it is food or something else. If classified as food, extracted food items are returned."
    )
    fun classifyPrompt(@ToolParam(description = "prompt") prompt: String): String {
        return """Classify the following prompt as 'food' or 'other'. If it is about food, extract dish name and/or ingredients. The prompt is=[${prompt}]
            |Your response should be in JSON format.
            |Do not include any explanations, only provide a RFC8259 compliant JSON response following this format without deviation.
            |Do not include markdown code blocks in your response.
            |Remove the ```json markdown from the output.
            |Here is the JSON Schema instance your output must adhere to:
            |```{
            |  "${'$'}schema" : "https://json-schema.org/draft/2020-12/schema",
            |    "type" : "object",
            |      "properties" : {
            |          "classification" : {
            |                "type" : "string",
            |                      "enum" : [ "FOOD", "OTHER" ]
            |                          },
            |                              "foodElements" : {
            |                                    "type" : "array",
            |                                          "items" : {
            |                                                  "type" : "string"
            |                                                        }
            |                                                            }
            |                                                              },
            |                                                                "additionalProperties" : false
            |                                                                }```
            |                                                                """.trimMargin()
    }



    @Tool(
        name = "find-dishes-service",
        description = "Select matching dishes for given food elements like meal or dish name and or ingredients"
    )
    fun menuSelection(@ToolParam(description = "food elements like meals, dishes or ingredients") foodElements: List<String>): List<String> {
        return restClient.get()
            .uri { uriBuilder ->
                val builder = uriBuilder.path("/ai/find-dishes")
                // Add each food element as a separate query parameter with the same name
                foodElements.forEach { element ->
                    builder.queryParam("foodElements", element)
                }
                builder.build()
            }
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .requiredBody()

    }

    @Tool(name = "order-dish-service", description = "Dish order service")
    fun orderDish(@ToolParam(description = "Dishes that will be ordered") order: OrderRequest): OrderResponse {
        return restClient.post()
            .uri("/ai/order-dish")
            .contentType(MediaType.APPLICATION_JSON)
            .body(order)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .requiredBody()
    }

    @Tool(
        name = "italian-meal-agent",
        description = "Suggests dishes and handles orders using a conversational waiter AI."
    )
    fun italianAgent(): String {
        return ITALIAN_AGENT_PROMPT
    }


    companion object {
        val ITALIAN_AGENT_PROMPT = """
You are an Italian waiter AI who assists customers in choosing and ordering dishes.

Here's how to behave:
- before looking for preferred meals first run the `classify-prompt-if-food-or-other` tool to understand whether the prompt is about a food preference or not.
- If the user gives food preferences or ingredients, use the `find-dishes-service` tool to find matching dishes.
- Propose ALL matching dishes from the `find-dishes-service` result.
- If the customer confirms a dish, call the `order-dish-service` tool.
- Once the order is placed, thank them and summarize the dish names with the estimated delivery time.

Only use the tools to gather or act on information. Do not invent dishes. Be polite, helpful, and speak in a friendly English tone.
""".trimIndent()

    }
}


enum class Classification {
    FOOD, OTHER
}

data class PromptClassification(val classification: Classification, val foodElements: List<String>)

data class OrderRequest(val meals: List<String>)

data class OrderResponse(val deliveredInMinutes: Int)


@Configuration
class McpConfig {

    @Bean
    fun fileTool(fileService:FileService, foodService: FoodService): List<ToolCallback> =
        ToolCallbacks.from(fileService, foodService).toList()

}