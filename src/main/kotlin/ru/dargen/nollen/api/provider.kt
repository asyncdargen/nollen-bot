package ru.dargen.nollen.api

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import ru.dargen.nollen.Executor
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

object AIProvider {

    private val Factories = mutableMapOf<String, AIFactory>()
    private val AIs: MutableMap<String, MutableList<AI>> = ConcurrentHashMap()
    private val Requests: Queue<AIRequest> = LinkedList()

    init {
        thread(isDaemon = true, name = "AI-Processor") {
            while (true) runCatching {
                synchronized(Requests) {
                    Requests.peek()?.let {
                        val ai = AIs[it.type]
                            ?.firstOrNull()
                            //lock mechanism
                            ?.apply { if (hasCooldown()) AIs[it.type]?.remove(this) } ?: return@let

                        Requests.remove(it)
                        it.activateHandler?.invoke()

                        ai.request(it.id, it.message).whenComplete { response, throwable ->
                            it.future.complete(
                                (response ?: ((throwable as CompletionException).cause)!!.localizedMessage.let(::AIResponse))
                            )

                            //release mechanism
                            if (ai.hasCooldown()) Executor.schedule(
                                { AIs[it.type]?.add(ai) },
                                ai.countdown?.toMillis() ?: 0L,
                                TimeUnit.MILLISECONDS
                            )
                        }
                    }
                }

                Thread.sleep(60)
            }.exceptionOrNull()?.printStackTrace()
        }
    }

    fun register(name: String, factory: AIFactory) =
        Factories.put(name, factory)

    fun load(presets: JsonArray) = presets.map(JsonElement::getAsJsonObject).forEach {
        val type = it.get("type").asString
        val ai = Factories[type]!!.create(it)

        AIs.getOrPut(type, ::arrayListOf).add(ai)
    }

    fun request(
        id: Long,
        type: String,
        message: String,
        activateHandler: (() -> Unit)? = null
    ) = CompletableFuture<AIResponse>().apply {
        Requests.add(AIRequest(id, type, message, this, activateHandler))
    }

}

data class AIRequest(
    val id: Long,
    val type: String,
    val message: String,
    val future: CompletableFuture<AIResponse>,
    val activateHandler: (() -> Unit)?
)

interface AIFactory {

    fun create(config: JsonObject): AI

}