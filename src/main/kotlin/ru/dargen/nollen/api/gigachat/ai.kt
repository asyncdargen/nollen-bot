package ru.dargen.nollen.api.gigachat

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import ru.dargen.nollen.api.*
import java.net.URL
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.regex.Matcher
import java.util.regex.Pattern

val ImageTagPattern = Pattern.compile("<image src=\\\"([\\s\\S]+)\\\" \\/>");

class GigaChatAI(
    private val spaceId: String,
    private val userId: String,
    private val cookies: String,
    cooldown: Long?
) : AbstractStorableAI(cooldown?.let(Duration::ofSeconds)) {

    override fun request(id: Long, completion: Completion): CompletableFuture<AIResponse> {
        val sessionId = UUID.nameUUIDFromBytes(id.toString().toByteArray())

        return CompletableFuture.supplyAsync {
            runCatching {
                GigaChat.request(
                    GigaChatRequestData(
                        sessionId,
                        completion.history.last().content,
                    ), spaceId, userId, cookies
                ).takeIf { println(it); it == "accepted" } ?: throw AIResponseException("Данный контекст уже имеет активный запрос!")

                //so dumb and stupid
                for (i in 0..240) {
                    val message = GigaChat.requestHistoryMessage(
                        GigaChatHistoryData(sessionId),
                        spaceId, userId, cookies
                    )

                    if (message.has("response_id")) {
                        val files = mutableMapOf<String, ByteArray>()
                        var content = message.get("data").asString
                        val matcher = ImageTagPattern.matcher(content)

                        matcher.takeIf(Matcher::find)?.let {
                            val name = it.group(1)
                            val data = URL("https://developers.sber.ru/studio/generated-images/$name").readBytes()
                            files[name.substring(name.lastIndexOf('/') + 1)] = data
                        }

                        content = matcher.replaceAll("")

                        return@runCatching AIResponse(content, files)
                    }

                    Thread.sleep(500)
                }

                AIResponse("Время выполнения запроса вышло!")

            }.run {
                getOrNull() ?: exceptionOrNull()?.let {
                    it.printStackTrace()
                    throw AIResponseException(it.localizedMessage)
                }
            }
        }
    }

}

object GigaChatAIFactory : AIFactory {
    override fun create(config: JsonObject) = GigaChatAI(
        config.get("space-id").asString,
        config.get("user-id").asString,
        config.get("cookies").asString,
        config.get("cooldown")?.takeUnless(JsonElement::isJsonNull)?.asLong
    )

}