package ru.dargen.nollen.discord

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.utils.FileUpload
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import ru.dargen.nollen.api.AIProvider
import ru.dargen.nollen.api.AIRequest
import ru.dargen.nollen.data.Chat
import ru.dargen.nollen.data.ChatTable
import ru.dargen.nollen.data.asUser
import ru.dargen.nollen.data.getChat
import ru.dargen.nollen.util.resolveContent

object ChatProcessor {

    val CloseButton = Button.of(
        ButtonStyle.DANGER, "close_chat",
        "Удалить чат", Emoji.fromFormatted("\uD83D\uDDD1\uFE0F")
    )
    val RepeatButton = Button.of(
        ButtonStyle.SUCCESS, "repeat_chat",
        "Повторить запрос", Emoji.fromFormatted("\uD83D\uDD01")
    )
    val ResetButton = Button.of(
        ButtonStyle.SECONDARY, "reset_chat",
        "Сбросить чат", Emoji.fromFormatted("\uD83E\uDDF9")
    )

    fun deleteChatRequest(event: ButtonInteractionEvent) {
        event.deferReply(true).queue()

        val user = event.user.asUser()
        val chat = event.message.getChat()

        if (user.isAdmin || user.id.value == chat?.ownerId) {
            event.channel.delete().queue()
            transaction {
                AIProvider.cancel(chat!!)
                ChatTable.deleteWhere { id eq chat.id }
            }
        } else {
            event.hook.sendMessage("Вы не можете удалить этот чат!").queue()
        }
    }

    fun resetChatRequest(event: ButtonInteractionEvent) {
        event.deferReply(true).queue()

        val user = event.user.asUser()
        val chat = event.message.getChat()

        if (user.isAdmin || user.id.value == chat?.ownerId) {
            chat!!.reset()
            transaction {
                AIProvider.cancel(chat)
                chat.save()
            }

            event.hook.sendMessage("Вы сбросили предыдущие сообщения в чате!")
                .queue()
        } else {
            event.hook.sendMessage("Вы не можете сбрасывать сообщения этого чата!")
                .queue()
        }
    }

    fun repeatChatRequest(event: ButtonInteractionEvent) {
        event.deferReply(true).queue()

//        val user = event.user.asUser()
        val chat = event.message.getChat()

        if (chat?.history?.lastOrNull()?.role == "assistant")
            chat.removeLatest()

        requestAIRaw(chat!!)
        event.hook.sendMessage("Отправлен повторный запрос!").queue()
    }

    fun requestAI(chat: Chat, event: MessageReceivedEvent) {
        requestAIRaw(chat, event.message.resolveContent())
    }

    fun requestAIRaw(chat: Chat, request: String? = null) {
        val channel = chat.asChannel()
        val message = channel.sendMessage("Запрос в очереди!").complete()

        AIProvider.request(chat) {
            when (it) {
                AIRequest.ProcessStatus.REQUESTING -> {
                    message.editMessage("Запрос выполняется...").queue()
                    request?.let { chat.appendMessage("user", it) }
                    chat.save()
                }

                AIRequest.ProcessStatus.CANCELLING -> message.editMessage("Запрос отменен!")
                    .setActionRow(RepeatButton, ResetButton, CloseButton)
                    .queue()
            }
        }?.thenAccept {
            if (!it.isError)
                chat.appendMessage("assistant", it.message)
            chat.save()

            var lastMessage: Message? = null

            it.message.takeIf(String::isNotBlank)
                ?.chunked(2000)
                ?.apply { channel.sendTyping().complete(); message.delete().queue() }
                ?.forEach { message -> lastMessage = channel.sendMessage(message).complete() }

            it.files
                .takeIf(Map<*, *>::isNotEmpty)
                ?.map { (name, data) -> FileUpload.fromData(data, name) }
                ?.apply { lastMessage = channel.sendFiles(this).complete() }

            lastMessage?.editMessageComponents(ActionRow.of(RepeatButton, ResetButton, CloseButton))?.queue()
        } ?: message.editMessage("В данном чате уже есть активный запрос!")
            .setActionRow(RepeatButton, ResetButton, CloseButton)
            .queue()
    }


}