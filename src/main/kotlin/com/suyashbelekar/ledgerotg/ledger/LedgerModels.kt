package com.suyashbelekar.ledgerotg.ledger

import com.suyashbelekar.ledgerotg.ledger.data.Amount
import java.time.LocalDate

data class Transaction(
    val date: LocalDate,
    val payee: String,
    val postings: List<Posting>
) {
    data class Posting(
        val account: AccountDetails,
        val amount: Amount
    )

    data class AccountDetails(
        val fullName: String,
        val alias: String? = null
    )
}

data class RegisterRow(
    val date: LocalDate,
    val payee: String,
    val account: Transaction.AccountDetails,
    val amount: Amount,
    val total: Amount
)