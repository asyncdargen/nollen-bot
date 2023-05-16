package ru.dargen.nollen

import com.google.gson.JsonElement
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.requests.GatewayIntent
import ru.dargen.nollen.api.AIProvider
import ru.dargen.nollen.api.gigachat.GigaChatAIFactory
import ru.dargen.nollen.api.openai.OpenAIFactory
import ru.dargen.nollen.discord.SimpleListener
import ru.dargen.nollen.util.Configuration
import ru.dargen.rest.RestClientFactory
import ru.dargen.rest.request.RequestOption
import java.util.concurrent.Executors

val RestClient = RestClientFactory.createHttpBuiltinClient().apply {
    baseRequest.withOption(RequestOption.REQUEST_TIMEOUT, 120000)
        .withHeader("User-Agent", "Nollen-Bot")
}
val Executor = Executors.newScheduledThreadPool(2)
val Channels = mutableMapOf<String, List<Long>>()

fun main() {
    val jda = JDABuilder.createDefault(Configuration["discord.token"].asString)
        .enableIntents(GatewayIntent.values().toList())
        .build()
        .awaitReady()

    jda.addEventListener(SimpleListener)
    jda.presence.setStatus(OnlineStatus.DO_NOT_DISTURB)

    Configuration["discord.channels"].asJsonObject.apply {
        keySet().forEach {
            Channels[it] = getAsJsonArray(it).map(JsonElement::getAsLong)
        }
    }

    AIProvider.register("openai", OpenAIFactory)
    AIProvider.register("gigachat", GigaChatAIFactory)

    AIProvider.load(Configuration["presets"].asJsonArray)

}
