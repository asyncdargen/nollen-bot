package ru.dargen.nollen

import com.google.gson.GsonBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.requests.GatewayIntent
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import ru.dargen.nollen.api.AIProvider
import ru.dargen.nollen.api.giga.GigaChatAIFactory
import ru.dargen.nollen.api.gpt.OpenAIFactory
import ru.dargen.nollen.data.ChatTable
import ru.dargen.nollen.data.UserTable
import ru.dargen.nollen.discord.ChatCreator
import ru.dargen.nollen.discord.SimpleListener
import ru.dargen.nollen.util.Configuration
import ru.dargen.rest.RestClientFactory
import ru.dargen.rest.request.RequestOption
import java.util.concurrent.Executors

val Gson = GsonBuilder().setPrettyPrinting().create()
val RestClient = RestClientFactory.createHttpBuiltinClient().apply {
    baseRequest.withOption(RequestOption.REQUEST_TIMEOUT, 120000)
        .withHeader("User-Agent", "Nollen-Bot")
}
val Executor = Executors.newScheduledThreadPool(2)

lateinit var JDA: JDA
    private set

fun main() {
    Database.connect(
        Configuration["database.url"].asString,
        driver = "com.mysql.cj.jdbc.Driver",
        user = Configuration["database.user"].asString,
        password = Configuration["database.password"].asString,
    )
    transaction {
        SchemaUtils.create(UserTable, ChatTable)
    }

    JDA = JDABuilder.createDefault(Configuration["discord.token"].asString)
        .enableIntents(GatewayIntent.values().toList())
        .build()
        .awaitReady()

    JDA.addEventListener(SimpleListener)
    JDA.presence.setStatus(OnlineStatus.DO_NOT_DISTURB)

    AIProvider.register("gpt", OpenAIFactory)
    AIProvider.register("giga", GigaChatAIFactory)

    AIProvider.load(Configuration["presets"].asJsonArray)

    ChatCreator.repostAll()

//    jda.getTextChannelById(1096750200959270976)!!.apply {
//        sendMessage("Скинь киску")
//            .addActionRow(Button.primary("ebat", "Тера пенис"))
//            .queue()
//    }
//
//    jda.addEventListener(object : ListenerAdapter() {
//        override fun onButtonInteraction(event: ButtonInteractionEvent) {
//            event.replyModal(Modal.create("create", "Создать Чат").apply {
//                addActionRow(
//                    TextInput.create("penis", "Член бобра", TextInputStyle.PARAGRAPH).setRequired(false).build()
//                )
//            }.build()).queue()
//        }
//    })

}


