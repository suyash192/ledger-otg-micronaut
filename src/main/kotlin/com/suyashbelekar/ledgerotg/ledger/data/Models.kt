package com.suyashbelekar.ledgerotg.ledger.data

import com.suyashbelekar.ledgerotg.pagination.Dir
import com.suyashbelekar.ledgerotg.pagination.Pageable
import com.suyashbelekar.ledgerotg.time.Interval
import com.suyashbelekar.ledgerotg.time.LocalDatePeriod
import java.time.format.DateTimeFormatter

data class Account(
    val name: String,
    val fullName: String,
    val balance: Amount,
    val subAccounts: List<Account>
)

data class Amount(
    val commodity: String,
    val value: Double
)

data class LedgerFilters(
    val accounts: List<String>,
    val payees: List<String>,
    val real: Boolean,
    val period: LocalDatePeriod?,
    val pageable: Pageable?,
    val interval: Interval?,
) {
    override fun toString(): String {
        val args = mutableListOf<String>()

        if (real) {
            args.add("--real")
        }

        if (period != null) {
            val dateFromString = period.dateStart.format(DateTimeFormatter.ISO_DATE)
            val dateToString =
                period.dateEnd.plusDays(1).format(DateTimeFormatter.ISO_DATE) // Needs to be inclusive
            args.add("""--period "from $dateFromString to $dateToString"""")
        }

        val sortOrder = pageable?.sort ?: listOf()

        if (sortOrder.isNotEmpty()) {
            val sortArg = sortOrder.joinToString(",", "--sort ") { order ->
                when (order.dir) {
                    Dir.ASC -> """"${order.property}""""
                    else -> """"-${order.property}""""
                }
            }

            args.add(sortArg)
        }

        if (accounts.isNotEmpty()) {
            args.add(accounts.joinToString(" ") {
                it.replace("\"", "\\\"")
            })
        }

        if (payees.isNotEmpty()) {
            args.add(payees.joinToString(" ") {
                val payeeEscaped = it.replace("\"", "\\\"")
                """@"$payeeEscaped""""
            })
        }

        return args.joinToString(" ")
    }
}

data class PeriodIncomeAndExpenses(
    val period: String,
    val income: Double,
    val expenses: Double
)

data class PeriodNetWorth(
    val period: String,
    val netWorth: Double
)

data class Differences(
    val assets: Double,
    val expenses: Double,
    val netWorth: Double
)