package com.suyashbelekar.ledgerotg.services

import com.suyashbelekar.ledgerotg.classifiers.ClassifiableTransaction
import com.suyashbelekar.ledgerotg.classifiers.TransactionClassifier
import com.suyashbelekar.ledgerotg.env.properties.AllProperties
import com.suyashbelekar.ledgerotg.ledger.Transaction
import com.suyashbelekar.ledgerotg.ledger.data.Amount
import com.suyashbelekar.ledgerotg.persistence.daos.Dao
import com.suyashbelekar.ledgerotg.persistence.models.TransactionRegex
import jakarta.inject.Singleton
import java.io.File
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

@Singleton
class TransactionService(
    private val transactionClassifier: TransactionClassifier,
    private val regexDao: Dao<TransactionRegex, Int>,
    private val ledgerProperties: AllProperties.LedgerProperties,
    private val clock: Clock
) {
    private val dateTimeFormatter = DateTimeFormatter.ISO_DATE

    fun insertTransaction(transactionString: String): Transaction? {
        return regexDao.findAll()
            .asSequence()
            .map { regex -> parseTransactionFromText(transactionString, regex) }
            .filterNotNull()
            .map { classifyTransaction(it) }
            .onEach { saveTransaction(it) }
            .firstOrNull()
    }

    private fun parseTransactionFromText(
        text: String,
        regex: TransactionRegex
    ): Transaction? {
        val valueRegex = regex.value.toRegex()

        if (!valueRegex.matches(text)) {
            return null
        }

        val accountNameFrom = regex.captures.accountFrom?.let { text.replace(valueRegex, it) } ?: ledgerProperties
            .accounts.from.default

        val accountNameTo = regex.captures.accountTo?.let { text.replace(valueRegex, it) } ?: ledgerProperties
            .accounts.to.default

        val payee = regex.captures.payee?.let { text.replace(valueRegex, it) } ?: ledgerProperties
            .accounts.payee.default

        val amount = regex.captures.amount
            ?.let { text.replace(valueRegex, it) }
            ?.toDoubleOrNull()
            ?: -0.0

        return Transaction(
            LocalDate.now(clock),
            payee,
            listOf(
                Transaction.Posting(
                    Transaction.AccountDetails(accountNameTo),
                    Amount(regex.captures.commodity ?: ledgerProperties.commodity.default, amount * -1)
                ),
                Transaction.Posting(
                    Transaction.AccountDetails(accountNameFrom),
                    Amount(ledgerProperties.commodity.default, amount)
                )
            )
        )
    }

    private fun classifyTransaction(transaction: Transaction): Transaction {
        val postingFirst = transaction.postings[0]
        val postingLast = transaction.postings[1]

        return if (postingFirst.account.fullName == "Expenses:Other") {
            val classifiedAccountNameTo = transactionClassifier.classify(
                ClassifiableTransaction(postingFirst.amount.value, transaction.payee)
            ) ?: postingFirst.account.fullName

            val postingFirstClassified = postingFirst.copy(
                account = postingFirst.account.copy(fullName = classifiedAccountNameTo)
            )

            transaction.copy(postings = listOf(postingFirstClassified, postingLast))
        } else {
            transaction
        }
    }

    private fun saveTransaction(transaction: Transaction) {
        val transactionString = with(transaction) {
            val builder = StringBuilder()

            builder.append(System.lineSeparator())
                .append(System.lineSeparator())
                .append(transaction.date.format(dateTimeFormatter))
                .append(" ")
                .append("\"${transaction.payee}\"")

            val maxLengthAccountName = transaction.postings.map { it.account.fullName.length }.maxOrNull() ?: 0
            val requiredLengthAccountName = (ceil(maxLengthAccountName / 8F).toInt() + 1) * 8

            for ((index, posting) in transaction.postings.withIndex()) {
                builder.append(System.lineSeparator()).append("\t").append(posting.account)

                if (index == 1 && transaction.postings.size == 2) {
                    continue
                }

                val paddingRequired = requiredLengthAccountName - posting.account.fullName.length
                val tabsRequired = ceil(paddingRequired / 8F).toInt()

                for (i in 0 until tabsRequired) {
                    builder.append("\t")
                }

                builder.append(posting.amount.commodity).append(posting.amount.value)
            }

            builder.append(" ")

            builder.toString()
        }

        File(ledgerProperties.filePath).appendText(transactionString, Charsets.UTF_8)
    }
}