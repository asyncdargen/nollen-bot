package ru.dargen.nollen.discord

import com.google.gson.JsonElement
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import org.jetbrains.exposed.sql.transactions.transaction
import ru.dargen.nollen.JDA
import ru.dargen.nollen.data.Chat
import ru.dargen.nollen.data.User
import ru.dargen.nollen.data.asUser
import ru.dargen.nollen.util.Configuration
import java.awt.Color

object ChatCreator {

    private val Buttons =
        Configuration["greeting.ai"].asJsonObject.asMap().mapValues { (_, value) -> value.asJsonObject }
            .mapValues { (key, json) ->
                Button.of(
                    ButtonStyle.valueOf(json.get("color").asString), "new_chat_$key",
                    json.get("name").asString,
                    Emoji.fromFormatted(json.get("icon").asString)
                )
            }

    private val GreetingMessage = MessageCreateBuilder().apply {
        addEmbeds(EmbedBuilder().apply {
            setColor(Color.GREEN)
            setTitle("Создать чат")
            setDescription("Здесь вы можете создать чат с одной из нейросетей.")
        }.build())
        addActionRow(Buttons.values)
    }.build()

    fun repostAll() {
        Configuration["discord.channels"]
            .asJsonArray
            .map(JsonElement::getAsLong)
            .forEach {
                delete(it)
                post(it)
            }
    }

    private fun delete(channelId: Long) {
        JDA.getTextChannelById(channelId)?.apply {
            deleteMessageById(latestMessageIdLong).queue()
        }
    }

    private fun post(channelId: Long) {
        JDA.getTextChannelById(channelId)
            ?.sendMessage(GreetingMessage)
            ?.queue()
    }

    fun newChatRequest(type: String, event: ButtonInteractionEvent) {
        event.replyModal(Modal.create("create_chat_$type", "Создать чат с ${Buttons[type]?.label}").apply {
            addActionRow(TextInput.create("title", "Название чата", TextInputStyle.SHORT).apply {
                minLength = 4
                maxLength = 100

                isRequired = false
                placeholder = "Пример: Написание плагина"
            }.build())
            addActionRow(TextInput.create("context", "Роль бота (предустановки чата)", TextInputStyle.PARAGRAPH).apply {
                isRequired = false
                placeholder = "Пример: Ты дворецкий бетмена"
            }.build())
        }.build()).queue()
    }

    fun createChatRequest(type: String, event: ModalInteractionEvent) {
        event.deferReply(true).queue()

        val user = event.user.asUser()

        val chatTitle = event.interaction.getValue("title")
            ?.asString
            ?.takeIf(String::isNotBlank)
        val chatContext = event.interaction.getValue("context")
            ?.asString
            ?.takeIf(String::isNotBlank)

        if (!user.canCreateChat()) {
            event.hook.sendMessage("У вас создано максимальное кол-во чатов. Удалите, чтобы создать еще один.")
                .queue()
        } else {
            val thread = user.createChat(event.channel.asTextChannel(), type, chatTitle, chatContext)
            event.hook.sendMessage("Чат создан - ${thread.asMention}")
                .queue()
        }
    }

    fun User.createChat(
        channel: TextChannel,
        type: String, title: String?, context: String?
    ) = transaction {
        val thread = channel.createThreadChannel(title ?: "Твой ${Buttons[type]?.label}", true)
            .setInvitable(true)
            .complete().apply {
                sendMessage("<@${this@createChat.id.value}>, чат создан и готов к работе.")
                    .addActionRow(ChatProcessor.CloseButton)
                    .queue()
            }

        Chat.new(thread.idLong) {
            ownerId = this@createChat.id.value

            this.type = type
            this.context = context

            reset()
        }

        thread
    }


}