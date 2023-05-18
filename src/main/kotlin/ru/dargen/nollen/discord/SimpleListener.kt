package ru.dargen.nollen.discord

import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import ru.dargen.nollen.data.getChat


object SimpleListener : ListenerAdapter() {

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot || event.channel.type != ChannelType.GUILD_PRIVATE_THREAD) return

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