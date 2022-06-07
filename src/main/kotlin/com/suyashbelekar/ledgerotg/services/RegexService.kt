package com.suyashbelekar.ledgerotg.services

import com.suyashbelekar.ledgerotg.persistence.daos.Dao
import com.suyashbelekar.ledgerotg.persistence.models.TransactionRegex
import jakarta.inject.Singleton

@Singleton
class RegexService(
    private val regexDao: Dao<@JvmSuppressWildcards TransactionRegex, @JvmSuppressWildcards Int>
) {
    fun insert(transactionRegex: TransactionRegex): TransactionRegex {
        return regexDao.insert(transactionRegex)
    }

    fun listAll(): List<TransactionRegex> {
        return regexDao.findAll()
    }

    fun findById(id: Int): TransactionRegex? {
        return regexDao.findById(id)
    }

    fun update(transactionRegex: TransactionRegex): TransactionRegex? {
        if (regexDao.findById(transactionRegex.id) == null) {
            return null
        }

        regexDao.update(transactionRegex)
        return transactionRegex
    }

    fun delete(id: Int): TransactionRegex? {
        return regexDao.deleteById(id)
    }
}