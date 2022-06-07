package com.suyashbelekar.ledgerotg.io

import jakarta.inject.Singleton
import java.io.File

interface FileSystem {
    fun writeText(file: File, text: String)
    fun readText(file: File): String?
}

@Singleton
class LocalFileSystem : FileSystem {
    override fun writeText(file: File, text: String) {
        file.writeText(text)
    }

    override fun readText(file: File): String? {
        return try {
            file.readText()
        } catch (e: Exception) { // TODO catch specific exception
            null
        }
    }
}