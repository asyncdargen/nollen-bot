package ru.dargen.nollen.data

import com.google.gson.reflect.TypeToken
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import ru.dargen.nollen.Gson
import ru.dargen.nollen.JDA
import kotlin.random.Random

object ChatTable : IdTable<Long>("chats") {

    override val id = long("id").entityId()
    override val primaryKey = PrimaryKey(id)

    val sessionId = long("session_id")
    val ownerId = long("owner_id").references(UserTable.id, ReferenceOption.CASCADE)
    val type = varchar("type", 32)
    val context = text("context").nullable()
    val history = text("history")

}

private val HistoryType = object : TypeToken<MutableList<ChatMessage>>() {}.type

data class ChatMessage(val role: String, val content: String)

class Chat(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Chat>(ChatTable)

    var sessionId by ChatTable.sessionId
    var ownerId by ChatTable.ownerId

    var type by ChatTable.type
    var context by ChatTable.context
    private var _history by ChatTable.history

    var history: MutableList<ChatMessage>
        get() = Gson.fromJson(_history, HistoryType)
        set(value) = this::_history.set(Gson.toJson(value))


    fun removeLatest() {
        transaction { history = history.apply { removeLast() } }
    }

    fun appendMessage(role: String, message: String) {
        transaction { history = history.apply { add(ChatMessage(role, message)) } }
    }

    fun reset() {
        transaction {
            _history = "[]"
            sessionId = Random.nextLong()
        }
    }

    fun save() = transaction {
        ChatTable.update({ ChatTable.id eq this@Chat.id.value }) {
            it[history] = this@Chat._history
            it[sessionId] = this@Chat.sessionId
        }
    }

    fun asChannel() = JDA.getThreadChannelById(id.value)!!

}

fun ThreadChannel.asChat() = transaction { Chat.findById(idLong) }

fun Message.getChat() = channel.asThreadChannel().asChat()