package com.suyashbelekar.ledgerotg.persistence.models

data class TransactionRegex(
    val id: Int = 0,
    val name: String,
    val value: String,
    val captures: Captures
) {
    data class Captures(
        val accountTo: String? = null,
        val accountFrom: String? = null,
        val payee: String? = null,
        val amount: String? = null,
        val commodity: String? = null
    )
}