package ru.dargen.nollen.api

import java.time.Duration
import java.util.concurrent.CompletableFuture

interface AI {

    val countdown: Duration?

    fun hasCooldown() = countdown != null

    fun request(id: Long, message: String): CompletableFuture<AIResponse>

}

data class AIResponse(val message: String, val files: Map<String, ByteArray> = emptyMap())

abstract class AbstractStorableAI(override val countdown: Duration? = null) : AI {

    override fun request(id: Long, message: String): CompletableFuture<AIResponse> {
        val completion = CompletionStorage.modify(id) { append("user", message) }

        return request(id, completion).whenComplete { result, _ ->
            result?.let { CompletionStorage.modify(id) { append("assistant", it.message) } }
        }
    }

    abstract fun request(id: Long, completion: Completion): CompletableFuture<AIResponse>

}

class AIResponseException(message: String) : RuntimeException(message)