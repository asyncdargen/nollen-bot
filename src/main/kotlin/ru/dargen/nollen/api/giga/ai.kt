package ru.dargen.nollen.api.giga

import com.google.gson.JsonObject
import ru.dargen.nollen.api.AIFactory
import ru.dargen.nollen.api.AIResponse
import ru.dargen.nollen.api.AIResponseException
import ru.dargen.nollen.api.ExceptionalAI
import ru.dargen.nollen.data.Chat
import ru.dargen.nollen.util.Timeout
import java.net.URL
import java.time.Duration
import java.util.*
import java.util.regex.Pattern

class GigaChatAI(
    private val spaceId: String,
    private val userId: String,
    private val cookies: String,
    cooldown: Long
) : ExceptionalAI("giga", Timeout(Duration.ofSeconds(cooldown))) {

    companion object {
        private const val ImageURL = "https://developers.sber.ru/studio/generated-images/"
        private val ImageTagPattern = Pattern.compile("<image src=\\\"([\\s\\S]+)\\\" \\/>")

        private const val LeastUUIDBits = 37412342379797094
    }

    override fun requestExceptionally(chat: Chat): AIResponse {
        runCatching {
            val sessionId = UUID(chat.sessionId, LeastUUIDBits)

            GigaChat.request(
                GigaChatRequestData(
                    sessionId,
                    chat.history.last().content,
                ), spaceId, userId, cookies
            ).takeIf { it == "accepted" }
                ?: throw AIResponseException("Данный контекст уже имеет активный запрос, повторите позже!")

            //so dumb and stupid
            for (i in 0..120) {
                val message = GigaChat.requestHistoryMessage(
                    GigaChatHistoryData(sessionId),
                    spaceId, userId, cookies
                )

                if (message.has("response_id"))
                    return message.get("data").asString.extractResponse()

                Thread.sleep(1000)
            }

            throw AIResponseException("Время выполнения запроса вышло, повторите позднее!")

        }.apply {
            exceptionOrNull()?.printStackTrace()
        }.getOrThrow()
    }

    private fun String.extractResponse(): AIResponse {
        val files = mutableMapOf<String, ByteArray>()
        val matcher = ImageTagPattern.matcher(this)

        while (matcher.find()) {
            val resource = matcher.group(1)
            val name = resource.substring(resource.lastIndexOf('/') + 1)
            val data = URL("$ImageURL$resource").readBytes()

            files[name] = data
        }

        return AIResponse(matcher.replaceAll(""), files)
    }

}

object GigaChatAIFactory : AIFactory {

    override fun create(config: JsonObject) = GigaChatAI(
        config.get("space-id").asString,
        config.get("user-id").asString,
        config.get("cookies").asString,

        config.get("cooldown").asLong
    )

}