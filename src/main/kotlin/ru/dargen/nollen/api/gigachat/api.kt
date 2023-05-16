package ru.dargen.nollen.api.gigachat

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import ru.dargen.nollen.RestClient
import ru.dargen.nollen.api.Completion
import ru.dargen.nollen.api.CompletionMessage
import ru.dargen.rest.annotation.*
import ru.dargen.rest.annotation.parameter.Body
import ru.dargen.rest.annotation.parameter.Header
import ru.dargen.rest.request.HttpMethod
import java.util.*

val GigaChat = RestClient.createController(GigaChatController::class.java)

@RequestMapping("https://developers.sber.ru/api/chatwm/api/client")
interface GigaChatController {

    @JsonQuery("result")
    @RequestMapping("/request", method = HttpMethod.POST)
    @RequestHeader(key = "Content-Type", value = "application/json")
    fun request(
        @Body request: GigaChatRequestData,
        @Header("space-id") spaceId: String,
        @Header("user-id") userId: String,
        @Header("Cookie") cookies: String
    ): String//CompletableFuture<String>

    @JsonQuery("messages[0]")
    @RequestMapping("/session_messages", method = HttpMethod.POST)
    @RequestHeader(key = "Content-Type", value = "application/json")
    fun requestHistoryMessage(
        @Body request: GigaChatHistoryData,
        @Header("space-id") spaceId: String,
        @Header("user-id") userId: String,
        @Header("Cookie") cookies: String
    ): JsonObject//CompletableFuture<String>

}

data class GigaChatRequestData(
    @SerializedName("session_id") val sessionId: UUID,
    @SerializedName("request_json") val message: String,

    @SerializedName("model_type") val model: String = "GigaChat_exp:v1.4.0",
    val preset: String = "default",
    @SerializedName("generate_alternatives") val generateAlternatives: Boolean = false,
)

data class GigaChatHistoryData(
    @SerializedName("session_id") val sessionId: UUID,
    @SerializedName("offset") val offset: Int = 0,
    @SerializedName("limit") val limit: Int = 1,
    @SerializedName("newer_first") val newerFirst: Boolean = true
)

data class CompletionRequestData(val messages: List<CompletionMessage>, val model: String = "gpt-3.5-turbo") {
    constructor(completion: Completion) : this(completion.history)
}
