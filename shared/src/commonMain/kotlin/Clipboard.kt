import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

expect fun waitClipboardText(): String

expect fun setClipboardText(text: String)

private var isSelfTrigger: Boolean = false

private val clipboardFlow = flow {
    while (currentCoroutineContext().isActive) {
        val text = waitClipboardText()
        if (isSelfTrigger) {
            isSelfTrigger = false
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
            isSelfTrigger = true
            setClipboardText(trimmed)
        }
    }
}
