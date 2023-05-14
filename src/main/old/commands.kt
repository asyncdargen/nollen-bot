package ru.dargen.nollen

//import net.dv8tion.jda.api.interactions.commands.Command
import com.theokanning.openai.completion.CompletionChoice
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction
import net.dv8tion.jda.api.utils.FileUpload
import ru.dargen.nollen.dispatcher.Dispatcher
import java.util.*

val CommandHandlers = mutableMapOf<String, SlashCommandInteractionEvent.() -> Unit>()
val ButtonHandlers = mutableMapOf<String, ButtonInteractionEvent.(List<String>) -> Unit>()
val Requests = mutableMapOf<UUID, String>()

fun CommandListUpdateAction.command(
    name: String, description: String,
    builder: SlashCommandData.() -> Unit,
    fallbackInteract: ButtonInteractionEvent.(List<String>) -> Unit,
    handler: SlashCommandInteractionEvent.() -> Unit
) {
    addCommands(Commands.slash(name, description).apply(builder))
    CommandHandlers[name] = handler
    ButtonHandlers[name] = fallbackInteract
}

fun dispatchText(timeout: Int, maxTokens: Int, model: String, prompt: String, replier: IReplyCallback) {

    replier.deferReply().complete()

    Dispatcher.dispatchText(timeout, maxTokens, model, prompt).whenComplete { result, throwable ->
        runCatching {
            when {
                throwable != null -> replier.hook.sendMessage("Ошибка выполнения: ${throwable.localizedMessage}")
                result == null -> replier.hook.sendMessage("Ответ не найден?!..(")
                else -> replier.hook.sendMessage(result.choices.joinToString("\n", transform = CompletionChoice::getText))
            }.addActionRow(Button.danger("chat ${UUID.randomUUID().apply { Requests[this] = "$timeout $maxTokens $model $prompt" }}", "Повторить")).queue()
        }.exceptionOrNull()?.printStackTrace()
    }
}

fun dispatchImage(timeout: Int, size: String, prompt: String, replier: IReplyCallback) {

    replier.deferReply().complete()

    Dispatcher.dispatchImage(timeout, size, prompt).whenComplete { result, throwable ->
        runCatching {
            when {
                throwable != null -> replier.hook.sendMessage("Ошибка генерации: ${throwable.localizedMessage}").apply { throwable.printStackTrace() }
                result == null -> replier.hook.sendMessage("?!..(")
                else -> replier.hook.sendFiles(
                    result.data.mapIndexed { index, it ->
                        FileUpload.fromData(
                            Base64.getDecoder().decode(it.b64Json.split(",").run { getOrNull(1) ?: get(0) }),
                            "attachment-${index}.png"
                        )
                    }
                )
            }.addActionRow(Button.danger("image ${UUID.randomUUID().apply { Requests[this] = "$timeout $size $prompt" }}", "Повторить")).queue()
        }.exceptionOrNull()?.printStackTrace()
    }
}

fun CommandListUpdateAction.register() = run {

    command(
        "chat", "Общаться с ботярой", {
            addOption(OptionType.INTEGER, "timeout", "Макс. время ожидания ответа", false)
            addOption(OptionType.INTEGER, "max-tokens", "Макс. кол-во слов", false, true)
            addOptions(OptionData(OptionType.STRING, "model", "Модель для генерации", false).apply {
                TextModels.forEach { addChoice(it, it) }
            })
            addOption(OptionType.STRING, "prompt", "Запрос")
        }, {
            val it = Requests[UUID.fromString(it[0])]!!.split(" ")
            val timeout = it[0].toInt()
            val maxTokens = it[1].toInt()
            val model = it[2]
            val prompt = it.drop(3)

            dispatchText(timeout, maxTokens, model, prompt.joinToString(" "), this)
        }, {
            val timeout = getOption("timeout", 60, OptionMapping::getAsInt)
            val maxTokens = getOption("max-tokens", 1024 + 512, OptionMapping::getAsInt)
            val model = getOption("model", TextModels.first(), OptionMapping::getAsString)
            val prompt = getOption("prompt", OptionMapping::getAsString)!!

            dispatchText(timeout, maxTokens, model, prompt, this)
        }
    )

    command(
        "image", "Сгенерировать картинку", {
            addOption(OptionType.INTEGER, "timeout", "Макс. время ожидания ответа", false)
            addOptions(
                OptionData(OptionType.STRING, "size", "Размер картинки", false)
                    .addChoice("256x256", "256x256")
                    .addChoice("512x512", "512x512")
                    .addChoice("1024x1024", "1024x1024")
            )
            addOption(OptionType.STRING, "prompt", "Запрос")
        }, {
            val it = Requests[UUID.fromString(it[0])]!!.split(" ")
            val timeout = it[0].toInt()
            val size = it[1]
            val prompt = it.drop(2)

            dispatchImage(timeout, size, prompt.joinToString(" "), this)
        }, {
            val timeout = getOption("timeout", 60, OptionMapping::getAsInt)
            val size = getOption("model", "512x512", OptionMapping::getAsString)
            val prompt = getOption("prompt", OptionMapping::getAsString)!!

            dispatchImage(timeout, size, prompt, this)
        }
    )



    complete()

    object : ListenerAdapter() {
        override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
            CommandHandlers[event.name]!!(event)
        }

        override fun onButtonInteraction(event: ButtonInteractionEvent) {
            val args = event.componentId.split(" ")
            ButtonHandlers[args[0]]?.invoke(event, args.drop(1))
        }
    }
}