package ru.dargen.nollen.discord

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Message.Attachment
import net.dv8tion.jda.api.entities.ThreadChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.utils.FileUpload
import ru.dargen.nollen.Channels
import ru.dargen.nollen.api.AIProvider
import ru.dargen.nollen.util.resolveFileText
import ru.dargen.nollen.util.resolveImageText
import ru.dargen.nollen.util.resolveURLsContents
import kotlin.math.min


object SimpleListener : ListenerAdapter() {

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot) return

        val channelId =
            if (event.isFromThread) event.channel.asThreadChannel().parentChannel.idLong
            else event.channel.idLong
        val type = Channels.entries.firstOrNull { (_, ids) -> channelId in ids }?.key ?: return

        var content = resolveCompletionRequest(event.message).takeIf(String::isNotBlank) ?: return

        val channel = if (!event.isFromThread) event.channel
            .asTextChannel()
            .createThreadChannel(
                content.run { if (length > 100) substring(0, 100) else this },
                event.messageId
            )
            .setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_3_DAYS)
            .complete()
        else event.channel.asThreadChannel()

        if (event.message.author.idLong == 1017841031972130866) {
            val replaces = content.toSet().shuffled().let {
                it.take(min(15, it.size))
            }
            replaces.forEach { content = content.replace(it, it + 10) }
        }

        channel.sendTyping().queue()
        val message = channel.sendMessage("Запрос в очереди...").complete()

        AIProvider.request(channel.idLong, type, content) {
            message.editMessage("Запрос выполняется...").queue()
        }.thenAccept {
            it.message.takeIf(String::isNotBlank)
                ?.chunked(2000)
                ?.apply { channel.sendTyping().complete(); message.delete().queue() }
                ?.forEach { message -> channel.sendMessage(message).complete() }

            it.files
                .takeIf(Map<*, *>::isNotEmpty)
                ?.map { (name, data) -> FileUpload.fromData(data, name) }
                ?.apply { channel.sendFiles(this).queue() }
        }
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
            else -> resolveFileText(attachment.fileName, attachment.retrieveInputStream().get())
        }
    }

}