package com.suyashbelekar.ledgerotg.time

import java.time.Clock
import java.time.LocalDate

data class LocalDatePeriod(
    val dateStart: LocalDate,
    val dateEnd: LocalDate
)

fun localDatePeriodOf(period: Period, clock: Clock): LocalDatePeriod {
    return when (period) {
        Period.CURRENT_MONTH -> with(LocalDate.now(clock)) {
            LocalDatePeriod(
                withDayOfMonth(1),
                withDayOfMonth(lengthOfMonth())
            )
        }
        Period.EPOCH_TO_NOW -> LocalDatePeriod(
            LocalDate.EPOCH,
            LocalDate.of(LocalDate.now().year + 1, 1, 1)
        )
    }
}

enum class Period {
    CURRENT_MONTH,
    EPOCH_TO_NOW
}

enum class Interval {
    MONTHLY,
    WEEKLY
}