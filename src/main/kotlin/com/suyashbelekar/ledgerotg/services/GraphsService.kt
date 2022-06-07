package com.suyashbelekar.ledgerotg.services

import com.suyashbelekar.ledgerotg.ledger.LedgerCli
import com.suyashbelekar.ledgerotg.ledger.data.Differences
import com.suyashbelekar.ledgerotg.ledger.data.PeriodIncomeAndExpenses
import com.suyashbelekar.ledgerotg.ledger.data.PeriodNetWorth
import com.suyashbelekar.ledgerotg.time.Interval
import jakarta.inject.Singleton

@Singleton
class GraphsService(
    private val ledgerCli: LedgerCli
) {
    fun incomeVsExpenses(interval: Interval): List<PeriodIncomeAndExpenses> {
        return ledgerCli.incomeVsExpensesReport(interval)
    }

    fun cashflow(interval: Interval): List<PeriodIncomeAndExpenses> {
        return ledgerCli.cashflowReport(interval)
    }

    fun netWorth(interval: Interval): List<PeriodNetWorth> {
        return ledgerCli.netWorthReport(interval)
    }

    fun difference(interval: Interval): Differences {
        return ledgerCli.differencesReport(interval)
    }
}