package ru.dargen.nollen.util

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.readText

object Configuration {

    lateinit var Json: JsonObject
        private set

    init {
        val file = Paths.get("config.json")

        if (file.exists()) {
            Json = GsonBuilder()
                .create()
                .fromJson(file.readText(), JsonObject::class.java)
        }
    }

    operator fun get(query: String) = ru.dargen.rest.util.Json.query(Json, query)

}