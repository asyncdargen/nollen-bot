package ru.dargen.nollen.dispatcher

import com.theokanning.openai.OpenAiService
import com.theokanning.openai.completion.CompletionRequest
import com.theokanning.openai.image.CreateImageRequest
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger

object Dispatcher {

    val Dispatchers: SwitchableList<String> = SwitchableList()
    val Working = AtomicInteger()

    fun add(dispatcher: String) = Dispatchers.add(dispatcher)

    fun free() = Dispatchers.switch()

    fun service(timeout: Int) = OpenAiService(free(), timeout)

    fun <T, R> startJob(timeout: Int, request: T, starter: (OpenAiService, T) -> R): CompletableFuture<R?> {
        Working.incrementAndGet()
        return CompletableFuture.supplyAsync {
            val result = runCatching { starter(service(timeout), request) }
            Working.decrementAndGet()
            result.getOrThrow()
        }
    }

    fun dispatchText(timeout: Int, maxTokens: Int, model: String, prompt: String) = startJob(
        timeout, CompletionRequest.builder()
            .maxTokens(maxTokens)
            .model(model)
            .prompt(prompt)
            .build(),
        OpenAiService::createCompletion
    ).apply { println("[Text/Code] timeout: $timeout, max-tokens: $maxTokens, model: $model, prompt: $prompt") }

    fun dispatchImage(timeout: Int, size: String, prompt: String) = startJob(
        timeout, CreateImageRequest.builder()
            .prompt(prompt)
            .size(size)
            .responseFormat("b64_json")
            .build(),
        OpenAiService::createImage
    ).apply { println("[Image] timeout: $timeout, size: $size, prompt: $prompt") }


}

