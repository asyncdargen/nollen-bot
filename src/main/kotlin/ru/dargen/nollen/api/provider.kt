package ru.dargen.nollen.api

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import ru.dargen.nollen.Executor
import ru.dargen.nollen.data.Chat
import java.util.*
import java.util.concurrent.CompletionException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object AIProvider {

    private val Factories = mutableMapOf<String, AIFactory>()
    private val Requests: MutableMap<String, Queue<AIRequest>> = hashMapOf()
    private val CompletingRequests: MutableSet<AIRequest> = Collections.newSetFromMap(ConcurrentHashMap())

    private val AIs: MutableList<AI> = arrayListOf()

    init {
        Executor.scheduleAtFixedRate({
            runCatching {
                synchronized(Requests) {
                    AIs.filter { it.timeout.isTimedOut() }.forEach { ai ->
                        Requests[ai.type]?.poll()?.let {
                            ai.timeout.set()
                            it.statusHandler?.invoke(AIRequest.ProcessStatus.REQUESTING)

                            CompletingRequests.add(it)

                            ai.request(it.chat).whenComplete { response, throwable ->
                                CompletingRequests.remove(it)

                                it.future.complete(
                                    response ?: AIResponse(
                                        (throwable as CompletionException).cause!!.localizedMessage,
                                        isError = true
                                    )
                                )
                            }
                        }
                    }
                }

            }.exceptionOrNull()?.printStackTrace()
        }, 60, 60, TimeUnit.MILLISECONDS)
    }

    fun register(type: String, factory: AIFactory) {
        Factories[type] = factory
        Requests.getOrPut(type, ::LinkedList)
    }

    fun load(presets: JsonArray) = presets.map(JsonElement::getAsJsonObject).forEach {
        val type = it.get("type").asString
        val ai = Factories[type]!!.create(it)

        AIs.add(ai)
    }

    fun request(chat: Chat, handler: ((AIRequest.ProcessStatus) -> Unit)? = null) = synchronized(Requests) {
        if (isRequesting(chat))
            return@synchronized null
        else AIRequest(chat, handler).apply(Requests[chat.type]!!::add).future
    }

    fun cancel(chat: Chat) {
        Requests[chat.type]
            ?.filter { it.chat.id.value == chat.id.value }
            ?.forEach {
                it.statusHandler?.invoke(AIRequest.ProcessStatus.CANCELLING)
                Requests[chat.type]?.remove(it)
            }
    }

    fun isRequesting(chat: Chat) = CompletingRequests.any { it.chat.id.value == chat.id.value }
            || Requests[chat.type]!!.any { it.chat.id.value == chat.id.value }

}

interface AIFactory {

    fun create(config: JsonObject): AI

}