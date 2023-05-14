package ru.dargen.nollen.completion

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.ThreadChannel
import ru.dargen.nollen.util.CompletionRequestData
import ru.dargen.nollen.util.Configuration
import ru.dargen.nollen.util.OpenApi
import ru.dargen.nollen.util.append
import ru.dargen.rest.response.ResponseException
import ru.dargen.rest.response.ResponseStatus
import java.util.*

private val Requsts: Queue<CompletionRequest> = LinkedList()

private fun pollRequest() = synchronized(Requsts, Requsts::poll)

class CompletionExecutor(private val key: String) : Thread() {

    init {
        isDaemon = true
        start()
    }

    override fun run() {
        while (true) runCatching {
            pollRequest()
                ?.execute(key)
                ?.let { sleep(2000) } ?: sleep(50)
        }.exceptionOrNull()?.printStackTrace()
    }

}

data class CompletionRequest(val channel: ThreadChannel, val message: Message) {

    init {
        Requsts.add(this)
    }

    fun execute(key: String) {
        runCatching {
            message.editMessage("Обработка...").complete()

            OpenApi.request(
                CompletionRequestData(listOf(Configuration.Main.ContextMessage) + channel.completionHistory.history),
                "Bearer $key"
            )
                .also { channel.modifyCompletionHistory { history.append("assistant", it) } }
                .chunked(2000)
                .apply { channel.sendTyping().complete(); message.delete().queue() }
                .forEach { message -> channel.sendMessage(message).complete() }

        }.exceptionOrNull()?.let {
            it.printStackTrace()
            message.editMessage("Oops! ${(it as? ResponseException)?.status?.asPlainText() ?: it.localizedMessage}")
                .queue()
        }
    }

    fun ResponseStatus.asPlainText() = when (this) {
        ResponseStatus.BAD_REQUEST -> "Данный контекст достиг максимума!"
        ResponseStatus.TOO_MANY_REQUESTS -> "Повторите еще раз позднее!"
        else -> "Ошибка при запросе: $this"
    }

}
