package com.suyashbelekar.ledgerotg.services

import com.suyashbelekar.ledgerotg.ledger.LedgerCli
import com.suyashbelekar.ledgerotg.ledger.RegisterRow
import com.suyashbelekar.ledgerotg.ledger.data.Account
import com.suyashbelekar.ledgerotg.ledger.data.LedgerFilters
import com.suyashbelekar.ledgerotg.pagination.Page
import jakarta.inject.Singleton

@Singleton
class ReportsService(
    private val ledgerCli: LedgerCli
) {
    fun balanceReport(ledgerFilters: LedgerFilters): Account {
        return ledgerCli.balanceReport(ledgerFilters)
    }

    fun registerReport(ledgerFilters: LedgerFilters): Page<RegisterRow> {
        return ledgerCli.registerReport(ledgerFilters, combineAdjacent = true, hideZero = true)
    }
}