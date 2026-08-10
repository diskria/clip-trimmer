import kotlinx.cinterop.*
import kotlinx.coroutines.runBlocking
import platform.posix.*

fun main() {
    runBlocking {
        observeClipboard()
    }
}

actual fun waitNextClip(): String {
    system("clipnotify > /dev/null 2>&1")
    return readFromCommand("xsel --clipboard --output")
}

actual fun setClipboardText(text: String) {
    sendToCommand("xsel --clipboard --input", text)
}

@OptIn(ExperimentalForeignApi::class)
private fun readFromCommand(command: String): String {
    val fp = popen(command, "r") ?: return ""
    val result = StringBuilder()
    memScoped {
        val buffer = allocArray<ByteVar>(1024)
        while (fgets(buffer, 1024, fp) != null) {
            result.append(buffer.toKString())
        }
    }
    pclose(fp)
    return result.toString()
}

@OptIn(ExperimentalForeignApi::class)
private fun sendToCommand(command: String, input: String) {
    val fp = popen(command, "w") ?: return
    try {
        fputs(input, fp)
        fflush(fp)
    } finally {
        pclose(fp)
    }
}
