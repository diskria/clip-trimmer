import kotlinx.cinterop.*
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
object Terminal {

    fun read(command: String): CommandResult =
        popen(command, "r")?.use { fp ->
            val result = StringBuilder()
            fp.forEachChunk { result.append(it.toKString()) }
            result.toString()
        }.let { CommandResult(it.orEmpty()) }

    value class CommandResult(val stdout: String)

    fun send(command: String, stdin: String) {
        popen(command, "w")?.use { fp ->
            fputs(stdin, fp)
            fflush(fp)
        }
    }

    fun exec(command: String, quiet: Boolean = false): Int =
        system(if (quiet) "$command > /dev/null 2>&1" else command)

    private inline fun <R> CPointer<FILE>.use(block: (CPointer<FILE>) -> R): R =
        try {
            block(this)
        } finally {
            pclose(this)
        }

    private inline fun CPointer<FILE>.forEachChunk(bufferSize: Int = 1024, action: (CPointer<ByteVar>) -> Unit) {
        memScoped {
            val buffer = allocArray<ByteVar>(bufferSize)
            while (fgets(buffer, bufferSize, this@forEachChunk) != null) {
                action(buffer)
            }
        }
    }
}
