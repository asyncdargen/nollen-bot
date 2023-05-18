package ru.dargen.nollen.data

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import net.dv8tion.jda.api.entities.User as DiscordUser

object UserTable : IdTable<Long>("users") {

    override val id = long("id").entityId()
    override val primaryKey = PrimaryKey(id)

    val isAdmin = bool("admin").default(false)

}

class User(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<User>(UserTable)

    val isAdmin by UserTable.isAdmin

    val maxChatsCount
        get() = if (isAdmin) Int.MAX_VALUE else 3
    val chatsCount
        get() = transaction { ChatTable.select { ChatTable.ownerId eq this@User.id.value }.count() }

    fun canCreateChat() = chatsCount < maxChatsCount

}

fun DiscordUser.asUser() = transaction { User.findById(idLong) ?: User.new(idLong) {    } }