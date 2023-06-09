package ru.dargen.nollen.api.gpt

import ru.dargen.nollen.Gson
import ru.dargen.nollen.RestClient
import ru.dargen.nollen.data.ChatMessage
import ru.dargen.rest.annotation.*
import ru.dargen.rest.annotation.parameter.Body
import ru.dargen.rest.annotation.parameter.Header
import ru.dargen.rest.request.HttpMethod

val OpenApi = RestClient.createController(OpenAIController::class.java)

@RequestMapping("https://api.openai.com/v1/chat")
interface OpenAIController {

    @JsonQuery("choices[0].message.content")
    @RequestMapping("/completions", method = HttpMethod.POST)
    @RequestHeader(key = "Content-Type", value = "application/json")
    fun requestCompletions(
        @Body request: OpenAIRequestData,
        @Header("Authorization") key: String
    ): String

}

data class OpenAIRequestData(val messages: List<ChatMessage>, val model: String = "gpt-3.5-turbo") {
    init {
        println(Gson.toJson(this))
    }
}