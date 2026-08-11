import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.io.OutputStream
import javax.inject.Inject

@DisableCachingByDefault(because = "Mutates system environment and controls external processes")
@UntrackedTask(because = "Installs files into user home directory and triggers systemctl")
abstract class SystemdInstallTask @Inject constructor(private val execOperations: ExecOperations) : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val executableBinary: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val generatedServiceFile: RegularFileProperty

    @get:Input
    abstract val binaryExecutableName: Property<String>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val installDir: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val systemdUserDir: DirectoryProperty

    @TaskAction
    fun install() {
        val os = DefaultNativePlatform.getCurrentOperatingSystem()
        if (!os.isLinux) {
            throw GradleException("Task 'systemdInstall' supports only Linux!")
        }

        fun runCmd(vararg args: String): Boolean {
            val result = execOperations.exec {
                commandLine(*args)
                isIgnoreExitValue = true
                standardOutput = OutputStream.nullOutputStream()
                errorOutput = OutputStream.nullOutputStream()
            }
            return result.exitValue == 0
        }

        if (!runCmd("which", "systemctl")) {
            throw GradleException("Task 'systemdInstall' failed: 'systemctl' command not found in PATH.")
        }

        val execName = binaryExecutableName.get()

        val binDir = installDir.get().asFile
        if (!binDir.exists()) binDir.mkdirs()

        val targetBinary = File(binDir, execName)
        executableBinary.get().asFile.copyTo(targetBinary, overwrite = true)
        targetBinary.setExecutable(true)
        logger.lifecycle("Binary successfully installed to: ${targetBinary.absolutePath}")

        val serviceDir = systemdUserDir.get().asFile
        if (!serviceDir.exists()) serviceDir.mkdirs()

        val serviceName = "$execName.service"
        val targetService = File(serviceDir, serviceName)
        generatedServiceFile.get().asFile.copyTo(targetService, overwrite = true)
        logger.lifecycle("Service unit installed to: ${targetService.absolutePath}")

        if (runCmd("systemctl", "--user", "daemon-reload")) {
            logger.lifecycle("Systemd daemon reloaded.")
        }
        if (runCmd("systemctl", "--user", "enable", serviceName)) {
            logger.lifecycle("Service $serviceName enabled.")
        }
        if (runCmd("systemctl", "--user", "restart", serviceName)) {
            logger.lifecycle("Service $serviceName successfully restarted!")
        } else {
            logger.warn("Failed to restart $serviceName.")
        }
    }
}
