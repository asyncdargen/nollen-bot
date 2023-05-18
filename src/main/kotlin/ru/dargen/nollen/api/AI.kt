package ru.dargen.nollen.api

import ru.dargen.nollen.data.Chat
import ru.dargen.nollen.util.Timeout
import ru.dargen.rest.response.ResponseException
import ru.dargen.rest.response.ResponseStatus
import java.util.concurrent.CompletableFuture

abstract class AI(val type: String, val timeout: Timeout) {

    abstract fun request(chat: Chat): CompletableFuture<AIResponse>

}

abstract class ExceptionalAI(type: String, timeout: Timeout) : AI(type, timeout) {

    abstract fun requestExceptionally(chat: Chat): AIResponse

    override fun request(chat: Chat): CompletableFuture<AIResponse> = CompletableFuture.supplyAsync {
        chat.runCatching(this::requestExceptionally).run {
            getOrNull() ?: exceptionOrNull()!!.let {
                it.printStackTrace()
                throw (it as? AIResponseException) ?: AIResponseException(
                    when {
                        it is ResponseException
                                && it.status == ResponseStatus.BAD_REQUEST -> "В чате слишком много сообщений!"

                        it is ResponseException
                                && it.status == ResponseStatus.TOO_MANY_REQUESTS -> "Слишком много запросов, повторите позже!"

                        it is ResponseException
                                && it.status.code / 500 == 1 -> "Ошибка на стороне сервера, повторите позже!"

                        else -> "Неизвестная ошибка ${it.localizedMessage}, повторите позже!"
                    }
                )
            }
        }
    }

}

data class AIRequest(
    val chat: Chat,
    val statusHandler: ((ProcessStatus) -> Unit)? = null,
    val future: CompletableFuture<AIResponse> = CompletableFuture()
) {

    enum class ProcessStatus {
        REQUESTING,
        CANCELLING
    }

}

data class AIResponse(
    val message: String,
    val files: Map<String, ByteArray> = emptyMap(),
    val isError: Boolean = false
)


class AIResponseException(message: String) : RuntimeException(message)