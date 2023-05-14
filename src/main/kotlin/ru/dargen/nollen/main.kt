package ru.dargen.nollen

import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.requests.GatewayIntent
import ru.dargen.nollen.completion.CompletionExecutor
import ru.dargen.nollen.util.Configuration
import kotlin.properties.Delegates

var SelfId by Delegates.notNull<Long>()

fun main() {
    val jda = JDABuilder.createDefault(Configuration.Main.Token)
        .enableIntents(GatewayIntent.values().toList())
        .build()
        .awaitReady()

    SelfId = jda.selfUser.idLong

    jda.addEventListener(SimpleListener)
    jda.presence.setStatus(OnlineStatus.DO_NOT_DISTURB)

    Configuration.Main.Keys.forEach(::CompletionExecutor)
}
