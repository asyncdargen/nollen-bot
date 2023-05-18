package ru.dargen.nollen.discord

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import ru.dargen.nollen.data.getChat


object SimpleListener : ListenerAdapter() {


//    override fun onMessageReceived(event: MessageReceivedEvent) {
//        if (event.author.isBot) return
//
//        val channelId =
//            if (event.isFromThread) event.channel.asThreadChannel().parentChannel.idLong
//            else event.channel.idLong
//        val type = Channels.entries.firstOrNull { (_, ids) -> channelId in ids }?.key ?: return
//
//        var content = resolveCompletionRequest(event.message).takeIf(String::isNotBlank) ?: return
//
//        val channel = if (!event.isFromThread) event.channel
//            .asTextChannel()
//            .createThreadChannel(
//                content.run { if (length > 100) substring(0, 100) else this },
//                true//event.messageId
//            )
//            .setInvitable(true)
//            .complete()
//            .apply {
//                addThreadMemberById(event.author.idLong).queue()
//            }
//        else event.channel.asThreadChannel()
//
//        if (event.message.author.idLong == 1017841031972130866) {
//            val replaces = content.toSet().shuffled().let {
//                it.take(min(15, it.size))
//            }
//            replaces.forEach { content = content.replace(it, it + 10) }
//        }
//
//        channel.sendTyping().queue()
//        val message = channel.sendMessage("Запрос в очереди...").complete()
//
//        AIProvider.request(channel.idLong, type, content) {
//            message.editMessage("Запрос выполняется...").queue()
//        }.thenAccept {
//            it.message.takeIf(String::isNotBlank)
//                ?.chunked(2000)
//                ?.apply { channel.sendTyping().complete(); message.delete().queue() }
//                ?.forEach { message -> channel.sendMessage(message).complete() }
//
//            it.files
//                .takeIf(Map<*, *>::isNotEmpty)
//                ?.map { (name, data) -> FileUpload.fromData(data, name) }
//                ?.apply { channel.sendFiles(this).queue() }
//        }
//    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot) return

        event.message.getChat()?.let { ChatProcessor.requestAI(it, event) }
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        val id = event.interaction.componentId

        when {
            id.startsWith("new_chat_") -> ChatCreator.newChatRequest(id.substring(9), event)

            id.startsWith("close_chat") -> ChatProcessor.deleteChatRequest(event)
            id.startsWith("reset_chat") -> ChatProcessor.resetChatRequest(event)
            id.startsWith("repeat_chat") -> ChatProcessor.repeatChatRequest(event)
        }
    }

    override fun onModalInteraction(event: ModalInteractionEvent) {
        val id = event.interaction.modalId

        when {
            id.startsWith("create_chat_") -> ChatCreator.createChatRequest(id.substring(12), event)
        }
    }
}