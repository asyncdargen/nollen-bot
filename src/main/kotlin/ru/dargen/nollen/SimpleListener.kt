package ru.dargen.nollen

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Message.Attachment
import net.dv8tion.jda.api.entities.ThreadChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import ru.dargen.nollen.completion.CompletionRequest
import ru.dargen.nollen.completion.modifyCompletionHistory
import ru.dargen.nollen.util.*


object SimpleListener : ListenerAdapter() {

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (((!event.isFromThread || event.channel.asThreadChannel().parentChannel.idLong !in Configuration.Main.Channels)
                    && event.channel.idLong !in Configuration.Main.Channels) || event.author.isBot
        ) return

        val content = resolveCompletionRequest(event.message).takeIf(String::isNotBlank) ?: return

        val channel = if (!event.isFromThread) event.channel
            .asTextChannel()
            .createThreadChannel(
                content.run { if (length > 100) substring(0, 100) else this },
                event.messageId
            )
            .setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_3_DAYS)
            .complete()
        else event.channel.asThreadChannel()

        channel.sendTyping().queue()
        val message = channel.sendMessage("Queuing request...").complete()


        channel.modifyCompletionHistory { history.append("user", content) }

        CompletionRequest(channel, message)
    }

    fun resolveCompletionRequest(message: Message): String {
        var content = resolveURLsContents(message.contentRaw)

        content += message.attachments
            .mapNotNull(this::resolveAttachment)
            .joinToString("\n", prefix = "\n")

        return content
    }

    fun resolveAttachment(attachment: Attachment): String? {
        return when {
            attachment.isImage -> resolveImageText(attachment.retrieveInputStream().get())
            attachment.isAudio || attachment.isVideo -> null
            else -> resolveFileText(attachment.retrieveInputStream().get())
        }
    }

}