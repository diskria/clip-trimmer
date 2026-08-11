import kotlinx.cinterop.*
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
object Terminal {

    fun read(command: String): String =
        popen(command, "r")?.use { fp ->
            val result = StringBuilder()
            fp.forEachChunk { result.append(it.toKString()) }
            result.toString()
        }.orEmpty()

    fun send(command: String, input: String) {
        popen(command, "w")?.use { fp ->
            fputs(input, fp)
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
