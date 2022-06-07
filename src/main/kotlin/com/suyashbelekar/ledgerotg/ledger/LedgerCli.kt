package com.suyashbelekar.ledgerotg.ledger

import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.suyashbelekar.ledgerotg.collections.collectUntilChanged
import com.suyashbelekar.ledgerotg.env.properties.AllProperties
import com.suyashbelekar.ledgerotg.io.Shell
import com.suyashbelekar.ledgerotg.ledger.data.*
import com.suyashbelekar.ledgerotg.pagination.Dir.DESC
import com.suyashbelekar.ledgerotg.pagination.Page
import com.suyashbelekar.ledgerotg.pagination.Pageable
import com.suyashbelekar.ledgerotg.pagination.Sort
import com.suyashbelekar.ledgerotg.time.Interval
import com.suyashbelekar.ledgerotg.time.Interval.MONTHLY
import com.suyashbelekar.ledgerotg.time.Interval.WEEKLY
import jakarta.inject.Singleton
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Singleton
class LedgerCli(
    private val shell: Shell,
    private val ledgerProperties: AllProperties.LedgerProperties
) {
    private val regexAmount = Regex("""^\s*?([^\s]+?)?\s*?([\-0-9.,]+)\s*?$""")

    private val numberFormat = NumberFormat.getNumberInstance()

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")

    private val zoneIdAsiaKolkata = ZoneId.of("Asia/Kolkata")

    private val csvMapper = CsvMapper().apply { registerModule(KotlinModule.Builder().build()) }

    private val objectReaderBalance by lazy {
        val csvSchema: CsvSchema = csvMapper.schemaFor(CsvRowBalance::class.java).withEscapeChar('\\')
        csvMapper.readerFor(CsvRowBalance::class.java).with(csvSchema)
    }

    private val objectReaderRegister by lazy {
        val csvSchema: CsvSchema = csvMapper.schemaFor(CsvRowRegister::class.java).withEscapeChar('\\')
        csvMapper.readerFor(CsvRowRegister::class.java).with(csvSchema)
    }

    private val objectReaderRegisterWithTotal by lazy {
        val csvSchema: CsvSchema = csvMapper.schemaFor(CsvRowRegisterWithTotal::class.java).withEscapeChar('\\')
        csvMapper.readerFor(CsvRowRegisterWithTotal::class.java).with(csvSchema)
    }

    private val objectReaderPeriodAmount by lazy {
        val csvSchema: CsvSchema = csvMapper.schemaFor(CsvRowPeriodAmount::class.java).withEscapeChar('\\')
        csvMapper.readerFor(CsvRowPeriodAmount::class.java).with(csvSchema)
    }

    private fun parseTransaction(
        list: List<CsvRowRegister>
    ): Transaction {
        val csvRowFirst = list.first()

        val timestamp = LocalDate.parse(csvRowFirst.date, dateTimeFormatter)
            .atStartOfDay(zoneIdAsiaKolkata)
            .toLocalDate()

        val postings = list.asSequence()
            .map { csvRowRegister ->
                val matchResultAmount = regexAmount.matchEntire(csvRowRegister.amount)
                    ?: throw RuntimeException("Error parsing amount: ${csvRowRegister.amount}")

                val commodity = matchResultAmount.groups[1]?.value ?: ledgerProperties.commodity.default
                val amountValue = numberFormat.parse(matchResultAmount.groups[2]!!.value).toDouble()

                Transaction.Posting(
                    Transaction.AccountDetails(csvRowRegister.accountName),
                    Amount(
                        commodity,
                        amountValue
                    )
                )
            }
            .toList()

        return Transaction(timestamp, csvRowFirst.payee, postings)
    }

    private fun parseRegisterRow(
        csvRowRegister: CsvRowRegisterWithTotal
    ): RegisterRow {
        val date = LocalDate.parse(csvRowRegister.date, dateTimeFormatter)
            .atStartOfDay(zoneIdAsiaKolkata)
            .toLocalDate()

        val account = Transaction.AccountDetails(csvRowRegister.accountName)

        val amount = parseAmount(csvRowRegister.amount)

        val total = parseAmount(csvRowRegister.total)

        return RegisterRow(date, csvRowRegister.payee, account, amount, total)
    }

    private fun parseAmount(amount: String): Amount {
        val matchResultAmount = regexAmount.matchEntire(amount)
            ?: throw RuntimeException("Error parsing amount: $amount")

        val commodity = matchResultAmount.groups[1]?.value ?: ledgerProperties.commodity.default

        val amountValue = numberFormat.parse(matchResultAmount.groups[2]!!.value).toDouble()

        return Amount(
            commodity,
            amountValue
        )
    }

    fun listTransactions(
        ledgerFilters: LedgerFilters
    ): Page<Transaction> {
        val process = shell.runCommand(
            "bash",
            "-c",
            """${ledgerProperties.bin.path} reg -f ${ledgerProperties.filePath} --format "true,%(quoted(date)),%(quoted(payee)),%(quoted(account)),%(quoted(amount_expr))\n%/false,%(quoted(date)),%(quoted(payee)),%(quoted(account)),%(quoted(amount_expr))\n" $ledgerFilters"""
        )

        val mappingIterator = objectReaderRegister.readValues<CsvRowRegister>(process.inputStream)

        val transactions = mutableListOf<Transaction>()

        val count = mappingIterator.asSequence()
            .collectUntilChanged { _, t2 -> !t2.firstPosting }
            .filter { csvRows ->
                val matchingEntry = csvRows.asSequence()
                    .flatMap { csvRowRegister ->
                        ledgerFilters.accounts.asSequence().map { account -> csvRowRegister.accountName to account }
                    }
                    .firstOrNull { it.first.startsWith(it.second) }

                ledgerFilters.accounts.isEmpty() || matchingEntry != null
            }
            .mapIndexed { index, list ->
                if (ledgerFilters.pageable == null
                    || index >= ledgerFilters.pageable.offset && transactions.size < ledgerFilters.pageable.size
                ) {
                    val transaction = parseTransaction(list)
                    transactions.add(transaction)
                }

                list
            }
            .count()

        return Page(transactions, count.toLong(), ledgerFilters.pageable)
    }

    fun balanceReport(
        ledgerFilters: LedgerFilters
    ): Account {
        val process = shell.runCommand(
            "bash",
            "-c",
            """${ledgerProperties.bin.path} balance -f "${ledgerProperties.filePath}" --format "%(quoted(account)),%(quoted(display_total))\n" --empty $ledgerFilters"""
        )

        val mappingIterator = objectReaderBalance.readValues<CsvRowBalance>(process.inputStream)

        val accountMap = mappingIterator.asSequence()
            .map { csvRowBalance ->
                val accountNameFull = csvRowBalance.accountName.ifEmpty { "Root" }

                val accountNameBase = accountNameFull.substringAfterLast(':')

                val matchResultAmount = regexAmount.matchEntire(csvRowBalance.amount)
                    ?: throw RuntimeException("Error parsing amount: ${csvRowBalance.amount}")

                val commodity = matchResultAmount.groups[1]?.value ?: ledgerProperties.commodity.default
                val amountValue = numberFormat.parse(matchResultAmount.groups[2]!!.value).toDouble()

                val balance = Amount(commodity, amountValue)

                accountNameFull to Account(
                    accountNameBase,
                    accountNameFull,
                    balance,
                    mutableListOf()
                )
            }
            .toMap()
            .toMutableMap()

        process.waitFor()

        if (process.exitCode() != 0) {
            throw IllegalStateException("An unexpected exit value received: ${process.exitCode()}")
        }

        if (accountMap.isEmpty()) {
            return Account("Root", "Root", Amount(ledgerProperties.commodity.default, 0.0), listOf())
        }

        return convertToAccountTree(accountMap)
    }

    fun registerReport(
        ledgerFilters: LedgerFilters,
        combineAdjacent: Boolean,
        hideZero: Boolean
    ): Page<RegisterRow> {
        val sort = listOf(Sort(DESC, "date"))
        val pageable = ledgerFilters.pageable?.copy(sort = sort) ?: Pageable(sort = sort)
        val ledgerFiltersWithSort = ledgerFilters.copy(pageable = pageable)

        val process = shell.runCommand(
            "bash",
            "-c",
            """${ledgerProperties.bin.path} reg -f ${ledgerProperties.filePath} --format "%(quoted(code)),%(quoted(date)),%(quoted(payee)),%(quoted(account)),%(quoted(amount_expr)),%(quoted(total_expr))\n" $ledgerFiltersWithSort"""
        )

        val registerRows = mutableListOf<RegisterRow>()

        val count = objectReaderRegisterWithTotal.readValues<CsvRowRegisterWithTotal>(process.inputStream)
            .asSequence()
            .map { parseRegisterRow(it) }
            // TODO fix bug where sequence with single entries are discarded due to collectUntilChanged operator
            .collectUntilChanged { t1, t2 -> combineAdjacent && t1.date == t2.date && t1.payee == t2.payee && t1.account == t2.account }
            .map { rows -> combineIntoSingle(rows) }
            .filter { !hideZero || it.amount.value != 0.0 }
            .mapIndexed { index, registerRow ->
                if (ledgerFilters.pageable == null ||
                    (index >= ledgerFilters.pageable.offset && registerRows.size < ledgerFilters.pageable.size)
                ) {
                    registerRows.add(registerRow)
                }

                registerRow
            }
            .count()

        return Page(registerRows, count.toLong(), ledgerFilters.pageable)
    }

    private fun combineIntoSingle(rows: List<RegisterRow>): RegisterRow {
        val netChange = rows.sumByDouble { it.amount.value }
        val lastRow = rows.last()
        return lastRow.copy(amount = lastRow.amount.copy(value = netChange))
    }

    private fun convertToAccountTree(accountMap: MutableMap<String, Account>): Account {
        val accountRoot = accountMap["Root"] ?: throw IllegalStateException("Root account not found")

        var accountDepth = 0
        do {
            val affected = accountMap.filter { it.key != "Root" }
                .filter { entry -> entry.key.count { it == ':' } == accountDepth }
                .onEach { accountEntry ->
                    val parentAccount = findParentAccount(accountMap, accountEntry.key)

                    val accountNameChild = if (
                        accountEntry.value.fullName.startsWith(parentAccount.fullName)
                    ) {
                        accountEntry.value.fullName.substring(parentAccount.fullName.length + 1)
                    } else {
                        accountEntry.value.fullName
                    }

                    val accountChild = accountEntry.value.copy(
                        name = accountNameChild
                    )

                    (parentAccount.subAccounts as MutableList).add(accountChild)
                }
                .count()

            accountDepth++
        } while (affected > 0)

        return accountRoot
    }

    private fun findParentAccount(accountMap: MutableMap<String, Account>, childAccount: String): Account {
        val parentAccountName = childAccount.substringBeforeLast(':', "Root")
        return accountMap[parentAccountName] ?: findParentAccount(accountMap, parentAccountName)
    }

    fun incomeVsExpensesReport(
        interval: Interval, cumulative: Boolean = false
    ): List<PeriodIncomeAndExpenses> {
        val intervalArg = when (interval) {
            MONTHLY -> "--monthly"
            WEEKLY -> "--weekly"
        }

        val cumulativeArg = if (cumulative) "-J" else "-j"

        val formatAmount = """%(quoted(date)),%(abs(quantity(scrub(display_amount))))\n"""
        val formatTotal = """%(quoted(date)),%(abs(quantity(scrub(display_total))))\n"""

        val processIncome = shell.runCommand(
            "bash",
            "-c",
            """${ledgerProperties.bin.path} reg -f ${ledgerProperties.filePath} $cumulativeArg --collapse --real --plot-amount-format "$formatAmount" --plot-total-format "$formatTotal" $intervalArg ^Income"""
        )

        val processExpenses = shell.runCommand(
            "bash",
            "-c",
            """${ledgerProperties.bin.path} reg -f ${ledgerProperties.filePath} $cumulativeArg --collapse --real --plot-amount-format "$formatAmount" --plot-total-format "$formatTotal" $intervalArg ^Expenses"""
        )

        val incomeMap = objectReaderPeriodAmount.readValues<CsvRowPeriodAmount>(processIncome.inputStream)
            .asSequence()
            .map { it.period to it }
            .toMap()

        val expensesMap = objectReaderPeriodAmount.readValues<CsvRowPeriodAmount>(processExpenses.inputStream)
            .asSequence()
            .map { it.period to it }
            .toMap()

        val list = mutableListOf<PeriodIncomeAndExpenses>()

        for (period in (incomeMap.keys + expensesMap.keys).sorted()) {
            val income = incomeMap[period]
            val expenses = expensesMap[period]

            val a1 = income?.amount ?: if (cumulative) list.lastOrNull()?.income ?: 0.0 else 0.0
            val a2 = expenses?.amount ?: if (cumulative) list.lastOrNull()?.expenses ?: 0.0 else 0.0

            list.add(PeriodIncomeAndExpenses(period, a1, a2))
        }

        return list
    }

    fun cashflowReport(
        interval: Interval
    ): List<PeriodIncomeAndExpenses> {
        return incomeVsExpensesReport(interval, cumulative = true)
    }

    fun netWorthReport(
        interval: Interval
    ): List<PeriodNetWorth> {
        val intervalArg = when (interval) {
            MONTHLY -> "--monthly"
            WEEKLY -> "--weekly"
        }

        val formatTotal = """%(quoted(date)),%(abs(quantity(scrub(display_total))))\n"""

        val processNetWorth = shell.runCommand(
            "bash",
            "-c",
            """${ledgerProperties.bin.path} reg -f ${ledgerProperties.filePath} -J --collapse --real --plot-total-format "$formatTotal" $intervalArg ^Assets ^Liabilities"""
        )

        return objectReaderPeriodAmount.readValues<CsvRowPeriodAmount>(processNetWorth.inputStream)
            .asSequence()
            .map { PeriodNetWorth(it.period, it.amount) }
            .toList()
    }

    fun differencesReport(
        interval: Interval
    ): Differences {
        val intervalArg = when (interval) {
            MONTHLY -> "--monthly"
            WEEKLY -> "--weekly"
        }

        val periodArg = when (interval) {
            MONTHLY -> """-p "this month""""
            WEEKLY -> """-p "this week""""
        }

        val formatAmount = """%(quoted(date)),%(quantity(scrub(display_amount)))\n"""

        val command =
            """${ledgerProperties.bin.path} reg -f ${ledgerProperties.filePath} -j --collapse --real --plot-amount-format "$formatAmount" $intervalArg $periodArg"""

        val processAssets = shell.runCommand(
            "bash",
            "-c",
            """$command ^Assets"""
        )

        val processExpenses = shell.runCommand(
            "bash",
            "-c",
            """$command ^Expenses"""
        )

        val processNetWorth = shell.runCommand(
            "bash",
            "-c",
            """$command ^Assets ^Liabilities"""
        )

        val amountAssets = objectReaderPeriodAmount.readValues<CsvRowPeriodAmount>(processAssets.inputStream)
            .asSequence()
            .map { it.amount }
            .firstOrNull() ?: 0.0

        val amountExpenses = objectReaderPeriodAmount.readValues<CsvRowPeriodAmount>(processExpenses.inputStream)
            .asSequence()
            .map { it.amount }
            .firstOrNull() ?: 0.0

        val amountNetWorth = objectReaderPeriodAmount.readValues<CsvRowPeriodAmount>(processNetWorth.inputStream)
            .asSequence()
            .map { it.amount }
            .firstOrNull() ?: 0.0

        return Differences(amountAssets, amountExpenses, amountNetWorth)
    }
}

