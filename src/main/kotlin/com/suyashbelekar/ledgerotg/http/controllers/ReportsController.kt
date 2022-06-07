package com.suyashbelekar.ledgerotg.http.controllers

import com.suyashbelekar.ledgerotg.ledger.data.Account
import com.suyashbelekar.ledgerotg.ledger.data.LedgerFilters
import com.suyashbelekar.ledgerotg.pagination.Pageable
import com.suyashbelekar.ledgerotg.pagination.Sort
import com.suyashbelekar.ledgerotg.services.ReportsService
import com.suyashbelekar.ledgerotg.time.Interval
import com.suyashbelekar.ledgerotg.time.LocalDatePeriod
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import java.time.LocalDate

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/reports")
class ReportsController(
    private val reportsService: ReportsService
) {
    @Get("/balance", produces = ["application/json"])
    fun balance(
        @QueryValue
        accounts: List<String>?,
        @QueryValue
        payees: List<String>?,
        @QueryValue(defaultValue = "false")
        real: Boolean,
        @QueryValue
        dateStart: LocalDate?,
        @QueryValue
        dateEnd: LocalDate?,
        @QueryValue(defaultValue = "0")
        pageNo: Int,
        @QueryValue(defaultValue = "20")
        size: Int,
        @QueryValue(defaultValue = "")
        sort: List<Sort>,
        @QueryValue
        interval: Interval?
    ): Account {
        val dateStart2 = dateStart ?: LocalDate.EPOCH
        val dateEnd2 = dateEnd ?: LocalDate.now()

        val ledgerFilters = LedgerFilters(
            accounts ?: listOf(),
            payees ?: listOf(),
            real,
            LocalDatePeriod(dateStart2, dateEnd2),
            Pageable(pageNo, size, sort),
            interval
        )

        return reportsService.balanceReport(ledgerFilters)
    }
}