package ru.dargen.nollen.util

import net.dv8tion.jda.api.entities.Message.Attachment

val Attachment.isAudio
    get() = contentType?.contains("audio") ?: false