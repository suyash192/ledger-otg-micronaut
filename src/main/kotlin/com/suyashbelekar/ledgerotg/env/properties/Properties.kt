package com.suyashbelekar.ledgerotg.env.properties

import io.micronaut.context.annotation.ConfigurationProperties

@ConfigurationProperties("otg")
class AllProperties {
    lateinit var app: AppProperties
    lateinit var ledger: LedgerProperties
    lateinit var auth: AuthProperties
    lateinit var regex: RegexProperties

    @ConfigurationProperties("app")
    class AppProperties {
        var port: Int = 8080
        lateinit var dataDirPath: String
    }

    @ConfigurationProperties("ledger")
    class LedgerProperties {
        lateinit var filePath: String
        lateinit var commodity: Commodity
        lateinit var accounts: Accounts
        lateinit var bin: Bin

        @ConfigurationProperties("commodity")
        class Commodity {
            lateinit var default: String
        }

        @ConfigurationProperties("accounts")
        class Accounts {
            lateinit var to: AccountTo
            lateinit var from: AccountFrom
            lateinit var payee: AccountPayee

            @ConfigurationProperties("to")
            class AccountTo {
                lateinit var default: String
            }

            @ConfigurationProperties("from")
            class AccountFrom {
                lateinit var default: String
            }

            @ConfigurationProperties("payee")
            class AccountPayee {
                lateinit var default: String
            }
        }

        @ConfigurationProperties("bin")
        class Bin {
            lateinit var path: String
        }
    }

    @ConfigurationProperties("auth")
    class AuthProperties {
        lateinit var credentials: Credentials
        var maxAttempts: Int = 3
        lateinit var jwt: Jwt

        @ConfigurationProperties("creds")
        class Credentials {
            lateinit var username: String
            lateinit var password: String
        }

        @ConfigurationProperties("jwt")
        class Jwt {
            lateinit var issuer: String
            lateinit var secret: String
        }
    }

    @ConfigurationProperties("regex")
    class RegexProperties {
        lateinit var filePath: String
    }
}