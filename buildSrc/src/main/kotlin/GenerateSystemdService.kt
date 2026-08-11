import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class GenerateSystemdService : DefaultTask() {

    @get:Input
    abstract val binaryExecutableName: Property<String>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val installDir: DirectoryProperty

    @get:OutputFile
    abstract val outputServiceFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val execName = binaryExecutableName.get()
        val execPath = installDir.get().file(execName).asFile.absolutePath

        val content = """
            [Unit]
            Description=$execName Service
            After=network.target

            [Service]
            Type=simple
            ExecStart=$execPath
            Restart=on-failure
            RestartSec=5s

            [Install]
            WantedBy=default.target
        """.trimIndent()

        val targetFile = outputServiceFile.get().asFile
        targetFile.parentFile.mkdirs()
        targetFile.writeText(content)
    }
}
