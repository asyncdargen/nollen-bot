package ru.dargen.nollen.util

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Message.Attachment
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import ru.dargen.nollen.discord.isAudio
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.imageio.ImageIO
import kotlin.io.path.nameWithoutExtension
import kotlin.streams.toList

private val Tesseract = net.sourceforge.tess4j.Tesseract().apply {
    setDatapath("tessdata")
    setLanguage(
        Files.list(Paths.get("tessdata"))
            .toList()
            .joinToString("+", transform = Path::nameWithoutExtension)
    )
}

private val TextElements = listOf(
    "h1", "h2", "h3", "h4", "h5", "h6", "p", "a", "span",
    "em", "strong", "blockquote", "pre", "code",
    "ul", "ol", "li", "dl", "dt", "dd", "label"
).joinToString(",")

fun Message.resolveContent(): String {
    var content = contentRaw
//    content = resolveURLsContents(contentRaw)

    content += attachments
        .mapNotNull(Attachment::resolveContent)
        .joinToString("\n", prefix = "\n")

    return content
}

private fun Message.Attachment.resolveContent(): String? {
    return when {
        isImage -> resolveImageText(retrieveInputStream().get())
        isAudio || isVideo -> null
        else -> resolveFileText(fileName, retrieveInputStream().get())
    }
}

fun resolveImageText(inputStream: InputStream) = "Текст с картинки: ${Tesseract.doOCR(ImageIO.read(inputStream))}"

fun resolveURLsContents(text: String) = text.split(" ")
    .joinToString(" ") { if (it.startsWith("http")) resolveWebSiteContent(it) else it }

fun resolveWebSiteContent(url: String) = runCatching {
    val document = Jsoup.connect(url).get()

    Thread.sleep(500)

    val textElements = document.select(TextElements)

    "Текст с сайта $url: ${textElements.joinToString("\n", transform = Element::text)}"
}.getOrNull() ?: url

fun resolveFileText(name: String, inputStream: InputStream) = "Содержание файла $name:${String(inputStream.readBytes())}"