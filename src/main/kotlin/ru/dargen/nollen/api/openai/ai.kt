package ru.dargen.nollen.api.openai

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import ru.dargen.nollen.api.*
import ru.dargen.rest.response.ResponseException
import ru.dargen.rest.response.ResponseStatus
import java.time.Duration
import java.util.concurrent.CompletableFuture

class OpenAI(private val key: String, cooldown: Long?) : AbstractStorableAI(cooldown?.let(Duration::ofSeconds)) {

    override fun request(id: Long, completion: Completion): CompletableFuture<AIResponse> {
        return CompletableFuture.supplyAsync {
            runCatching {
                AIResponse(OpenApi.requestCompletions(OpenAIRequestData(completion), "Bearer $key"))
            }.run {
                getOrNull() ?: exceptionOrNull()?.let {
                    throw AIResponseException(when {
                        it is ResponseException
                                && it.status == ResponseStatus.BAD_REQUEST -> "Данный контекст достиг максимума!"

                        it is ResponseException
                                && it.status == ResponseStatus.TOO_MANY_REQUESTS -> "Слишком много запросов повторите позже!"

                        else -> "Ошибка при запросе: ${it.localizedMessage}"
                    })
                }
            }
        }
    }

}

object OpenAIFactory : AIFactory {
    override fun create(config: JsonObject) = OpenAI(
        config.get("key").asString,
        config.get("cooldown").takeUnless(JsonElement::isJsonNull)?.asLong
    )

}