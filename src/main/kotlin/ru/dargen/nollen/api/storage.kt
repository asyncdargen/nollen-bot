package ru.dargen.nollen.api

import com.google.gson.GsonBuilder
import net.dv8tion.jda.api.entities.ThreadChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

typealias CompletionHistory = MutableList<CompletionMessage>

object CompletionStorage {

    private val Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    private val Folder = Paths.get("completions").apply { takeUnless(Path::exists)?.createDirectories() }

    fun get(id: Long) = Folder.resolve("$id.completion")
        .takeIf(Files::exists)
        ?.run { Gson.fromJson(readText(), Completion::class.java) }
        ?: Completion()

    fun push(id: Long, completion: Completion) = Folder.resolve("$id.completion")
        .writeText(Gson.toJson(completion))

    fun modify(id: Long, handler: CompletionHistory.() -> Unit) = get(id)
        .apply { handler(history) }
        .also { push(id, it) }

}

data class Completion(val history: CompletionHistory = mutableListOf())

data class CompletionMessage(val role: String, val content: String)

fun MutableList<CompletionMessage>.append(role: String, content: String) = add(CompletionMessage(role, content))

val ThreadChannel.completionHistory get() = CompletionStorage.get(idLong)
fun ThreadChannel.modifyCompletionHistory(handler: CompletionHistory.() -> Unit) = CompletionStorage.modify(idLong, handler)