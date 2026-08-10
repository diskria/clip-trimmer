plugins {
    alias(libs.plugins.kmp)
}

kotlin {
    linuxX64 {
        binaries {
            executable {
                entryPoint = "main"
                baseName = "clip-trimmer"
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines)
        }
    }
}

tasks.register("systemdInstall") {
    group = "installation"
    description = "Copies the release binary to ~/.local/bin and restarts the systemd service"

    dependsOn("linkReleaseExecutableLinuxX64")

    doLast {
        val userHome = System.getProperty("user.home")
        val targetDir = File("$userHome/.local/bin")
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val buildBinary = layout.buildDirectory.file("bin/linuxX64/releaseExecutable/clip-trimmer.kexe").get().asFile
        val targetBinary = File(targetDir, "clip-trimmer")

        if (buildBinary.exists()) {
            buildBinary.copyTo(targetBinary, overwrite = true)
            targetBinary.setExecutable(true)
            println("Binary successfully installed to: ${targetBinary.absolutePath}")

            val process = ProcessBuilder("systemctl", "--user", "restart", "clip-trimmer.service").start()
            if (process.waitFor() == 0) {
                println("Service clip-trimmer.service successfully restarted!")
            } else {
                println("Failed to restart clip-trimmer.service (it might not be enabled or created yet).")
            }
        } else {
            error("Binary file not found at: ${buildBinary.absolutePath}")
        }
    }
}
