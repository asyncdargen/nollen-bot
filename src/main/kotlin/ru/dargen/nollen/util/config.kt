package ru.dargen.nollen.util

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import ru.dargen.nollen.completion.CompletionMessage
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.readText

class Configuration {

    companion object {

        lateinit var Main: Configuration

        init {
            val file = Paths.get("config.json")

            if (file.exists()) {
                Main = GsonBuilder()
                    .create()
                    .fromJson(file.readText(), Configuration::class.java)
            }
        }

    }

    @SerializedName("token")
    lateinit var Token: String

    @SerializedName("channels")
    lateinit var Channels: List<Long>

    @SerializedName("context")
    lateinit var Context: List<String>
    val ContextMessage by lazy { CompletionMessage("system", Context.joinToString(" ")) }

    @SerializedName("keys")
    lateinit var Keys: List<String>

}