package ru.dargen.nollen.discord

import net.dv8tion.jda.api.entities.Message.Attachment

val Attachment.isAudio
    get() = contentType?.contains("audio") ?: false