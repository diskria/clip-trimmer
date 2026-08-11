import kotlinx.coroutines.runBlocking

fun main() {
    runBlocking {
        observeClipboard()
    }
}

actual fun waitClipboardText(): String {
    Terminal.exec("clipnotify", quiet = true)
    return Terminal.read("xclip -selection clipboard -o").stdout
}

actual fun setClipboardText(text: String) {
    Terminal.send("xclip -selection clipboard", stdin = text)
}
