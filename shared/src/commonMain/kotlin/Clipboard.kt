import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

expect fun waitClipboardText(): String

expect fun setClipboardText(text: String)

private var selfTriggeredText: String? = null

private val clipboardFlow = flow {
    while (currentCoroutineContext().isActive) {
        val text = waitClipboardText()
        if (text == selfTriggeredText) {
            selfTriggeredText = null
            continue
        }
        emit(text)
    }
}

suspend fun observeClipboard() {
    clipboardFlow.collect { text ->
        val trimmed = when {
            text.isBlank() -> return@collect
            text.contains('\n') -> text.trimIndent()
            else -> text.trim()
        }
        if (trimmed != text) {
            selfTriggeredText = trimmed
            setClipboardText(trimmed)
        }
    }
}
