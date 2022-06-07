package com.suyashbelekar.ledgerotg

import io.micronaut.context.ApplicationContext
import io.micronaut.runtime.Micronaut
import io.micronaut.runtime.Micronaut.run
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info

@OpenAPIDefinition(
    info = Info(
        title = "ledger-otg-micronaut",
        version = "0.0"
    )
)
object Api

fun main(args: Array<String>) {
//    run("local", "dev")
//    ApplicationContext.run()
    Micronaut.build(*args)
        .defaultEnvironments("dev", "local")
        .start()
}


