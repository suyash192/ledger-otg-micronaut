package com.suyashbelekar.ledgerotg.ledger.data

import com.fasterxml.jackson.annotation.JsonPropertyOrder

@JsonPropertyOrder("accountName", "amount")
data class CsvRowBalance(
    val accountName: String,
    val amount: String
)

@JsonPropertyOrder("firstPosting", "date", "payee", "accountName", "amount")
data class CsvRowRegister(
    val firstPosting: Boolean,
    val date: String,
    val payee: String,
    val accountName: String,
    val amount: String
)

@JsonPropertyOrder("code", "date", "payee", "accountName", "amount", "total")
data class CsvRowRegisterWithTotal(
    val code: String,
    val date: String,
    val payee: String,
    val accountName: String,
    val amount: String,
    val total: String
)

@JsonPropertyOrder("period", "amount")
data class CsvRowPeriodAmount(
    val period: String,
    val amount: Double
)