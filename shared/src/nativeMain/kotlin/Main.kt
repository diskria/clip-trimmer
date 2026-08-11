import kotlinx.coroutines.runBlocking

fun main() {
    runBlocking {
        observeClipboard()
    }
}

actual fun waitClipboardText(): String {
    Terminal.exec("clipnotify > /dev/null 2>&1")
    return Terminal.read("xsel --clipboard --output")
}

actual fun setClipboardText(text: String) {
    Terminal.send("xsel --clipboard --input", text)
}
