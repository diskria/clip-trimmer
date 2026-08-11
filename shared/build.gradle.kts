plugins {
    alias(libs.plugins.kmp)
}

val binaryName = "clip-trimmer"

kotlin {
    linuxX64 {
        binaries {
            executable {
                entryPoint = "main"
                baseName = binaryName
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines)
        }
    }
}

val releaseExecutable = kotlin.linuxX64().binaries.getExecutable("release")
val userHomeProvider = providers.systemProperty("user.home")
val installBinDir = objects.directoryProperty().fileProvider(
    userHomeProvider.map { File(it, ".local/bin") }
)
val systemdUserConfigDir = objects.directoryProperty().fileProvider(
    userHomeProvider.map { File(it, ".config/systemd/user") }
)

val generateService = tasks.register<GenerateSystemdService>("generateSystemdService") {
    group = "installation"
    description = "Generates systemd service file"

    binaryExecutableName.set(binaryName)
    installDir.set(installBinDir)
    outputServiceFile.set(layout.buildDirectory.file("generated/systemd/$binaryName.service"))
}

tasks.register<SystemdInstallTask>("systemdInstall") {
    group = "installation"
    description = "Copies release binary, installs systemd user service, and restarts it"

    binaryExecutableName.set(binaryName)
    installDir.set(installBinDir)
    systemdUserDir.set(systemdUserConfigDir)
    executableBinary.set(releaseExecutable.outputFile)
    generatedServiceFile.set(generateService.flatMap { it.outputServiceFile })

    dependsOn(releaseExecutable.linkTaskProvider)
}
