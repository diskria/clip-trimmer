import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

expect fun waitNextClip(): String

expect fun setClipboardText(text: String)

private val clipboardFlow = flow {
    while (currentCoroutineContext().isActive) {
        val clip = waitNextClip()
        if (clip.isNotBlank()) {
            emit(clip)
        }
    }
}

suspend fun observeClipboard() {
    clipboardFlow.collect { text ->
        val trimmed = if (text.contains('\n')) {
            text.trimIndent()
        } else {
            text.trim()
        }
        if (trimmed != text) {
            setClipboardText(trimmed)
        }
    }
}
