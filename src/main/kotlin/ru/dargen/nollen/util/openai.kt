package ru.dargen.nollen.util

import ru.dargen.nollen.completion.CompletionMessage
import ru.dargen.rest.RestClientFactory
import ru.dargen.rest.annotation.JsonQuery
import ru.dargen.rest.annotation.RequestHeader
import ru.dargen.rest.annotation.RequestMapping
import ru.dargen.rest.annotation.RequestMethod
import ru.dargen.rest.annotation.parameter.Body
import ru.dargen.rest.annotation.parameter.Header
import ru.dargen.rest.request.HttpMethod

val OpenApi = RestClientFactory.createHttpBuiltinClient()
    .apply { baseRequest.withOption(ru.dargen.rest.request.RequestOption.REQUEST_TIMEOUT, 120000) }
    .createController(OpenAI::class.java)

@RequestMapping("https://api.openai.com/v1/chat/completions")
interface OpenAI {

    @RequestMethod(HttpMethod.POST)
    @JsonQuery("choices[0].message.content")
    @RequestHeader(key = "Content-Type", value = "application/json")
    fun request(@Body request: CompletionRequestData, @Header("Authorization") key: String): String

}

data class CompletionRequestData(val messages: List<CompletionMessage>, val model: String = "gpt-3.5-turbo")

fun MutableList<CompletionMessage>.append(role: String, content: String) = add(CompletionMessage(role, content))
