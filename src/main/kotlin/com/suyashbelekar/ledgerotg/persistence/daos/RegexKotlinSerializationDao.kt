package com.suyashbelekar.ledgerotg.persistence.daos

import com.suyashbelekar.ledgerotg.io.FileSystem
import com.suyashbelekar.ledgerotg.persistence.models.TransactionRegex
import jakarta.inject.Singleton
import java.io.File

const val regexDataFileName = "regex.data"

@Singleton
class RegexKotlinSerializationDao(
    private val fileSystem: FileSystem,
    dataDir: File
) : Dao<TransactionRegex, Int> {
    private val dataFile = File(dataDir, regexDataFileName)

    private lateinit var _regexes: MutableMap<Int, TransactionRegex>
    private val regexes: MutableMap<Int, TransactionRegex>
        get() {
            if (!this::_regexes.isInitialized) {
                _regexes = deserialize()
            }
            return _regexes
        }

    override fun findAll(): List<TransactionRegex> = regexes.values.toList()

    override fun findById(id: Int): TransactionRegex? = regexes[id]

    override fun insert(element: TransactionRegex): TransactionRegex {
        // Store the value
        val maxId = if (regexes.keys.isEmpty()) 0 else regexes.keys.maxOf { it }
        val elementWithId = element.copy(id = maxId + 1)
        regexes[elementWithId.id] = elementWithId

        serialize()

        return elementWithId
    }

    override fun update(element: TransactionRegex): Boolean {
        regexes[element.id] ?: return false
        regexes[element.id] = element

        serialize()

        return true
    }

    override fun deleteById(id: Int): TransactionRegex? {
        val regex = regexes.remove(id)

        if (regex != null) {
            serialize()
        }

        return regex
    }

    private fun serialize() {
//        val text = Json.encodeToString(regexes)
//        fileSystem.writeText(dataFile, text)
        TODO()
    }

    private fun deserialize(): MutableMap<Int, TransactionRegex> {
//        val text = fileSystem.readText(dataFile) ?: return mutableMapOf()
//        return Json.decodeFromString(text)
        TODO()
    }
}