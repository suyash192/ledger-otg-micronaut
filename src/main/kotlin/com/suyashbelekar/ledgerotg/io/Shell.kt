package com.suyashbelekar.ledgerotg.io

import jakarta.inject.Singleton
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit

interface Shell {
    fun runCommand(vararg commands: String, workingDir: File = File("/tmp")): Process
}

class Process(
    val inputStream: InputStream,
    val waitFor: () -> Unit,
    val exitCode: () -> Int
)

@Singleton
class LocalShell : Shell {
    override fun runCommand(vararg commands: String, workingDir: File): Process {
        val proc = ProcessBuilder(*commands)
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        return Process(
            proc.inputStream,
            { proc.waitFor(60, TimeUnit.MINUTES) },
            { proc.exitValue() }
        )
    }
}