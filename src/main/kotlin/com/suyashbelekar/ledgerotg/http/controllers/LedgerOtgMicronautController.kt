package com.suyashbelekar.ledgerotg.http.controllers

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/ledgerOtgMicronaut")
class LedgerOtgMicronautController {
    @Get(uri = "/", produces = ["application/json"])
    fun index(): Test {
        return Test("Example Response")
    }
}

data class Test(val a: String)