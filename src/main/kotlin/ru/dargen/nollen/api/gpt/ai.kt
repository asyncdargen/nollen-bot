package ru.dargen.nollen.api.gpt

import com.google.gson.JsonObject
import ru.dargen.nollen.api.AIFactory
import ru.dargen.nollen.api.AIResponse
import ru.dargen.nollen.api.ExceptionalAI
import ru.dargen.nollen.data.Chat
import ru.dargen.nollen.data.ChatMessage
import ru.dargen.nollen.util.Timeout
import java.time.Duration

class GPTChatAI(private val key: String, cooldown: Long) :
    ExceptionalAI("gpt", Timeout(Duration.ofSeconds(cooldown))) {

    override fun requestExceptionally(chat: Chat) = AIResponse(
        OpenApi.requestCompletions(
            OpenAIRequestData(chat.context.asContextMessage() + chat.history),
            "Bearer $key"
        )
    )


    private fun String?.asContextMessage() =
        if (this == null) emptyList() else listOf(ChatMessage("system", this))

}

object OpenAIFactory : AIFactory {

    override fun create(config: JsonObject) = GPTChatAI(
        config.get("key").asString,
        config.get("cooldown").asLong
    )

}