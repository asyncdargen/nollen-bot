package ru.dargen.nollen.completion

import com.google.gson.GsonBuilder
import net.dv8tion.jda.api.entities.ThreadChannel
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

object CompletionStorage {

    private val Gson = GsonBuilder().setPrettyPrinting().create()
    private val Folder = Paths.get("completions").apply { if (!exists()) createDirectories() }

    fun get(id: Long) = Folder.resolve("$id.completion")
        .takeIf(Files::exists)
        ?.run { Gson.fromJson(readText(), Completion::class.java) }
        ?: Completion(mutableListOf())

    fun push(id: Long, completion: Completion) = Folder.resolve("$id.completion").writeText(Gson.toJson(completion))

    fun modify(id: Long, handler: Completion.() -> Unit) = get(id).apply(handler).run { push(id, this) }

}

data class Completion(val history: MutableList<CompletionMessage>)

data class CompletionMessage(val role: String, val content: String)

val ThreadChannel.completionHistory get() = CompletionStorage.get(idLong)
fun ThreadChannel.modifyCompletionHistory(handler: Completion.() -> Unit) = CompletionStorage.modify(idLong, handler)