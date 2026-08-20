package dev.anychain.api

import dev.anychain.core.Blockchain
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.KotlinLoggingConfiguration
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing

private val logger = KotlinLogging.logger {}

fun main() {
    KotlinLoggingConfiguration.logStartupMessage = false

    val dbPath = System.getenv("ANYCHAIN_DB") ?: "/tmp/anychain"
    val port = (System.getenv("PORT") ?: "3000").toInt()

    val chain = ChainHandle(Blockchain.open(dbPath))
    Runtime.getRuntime().addShutdownHook(Thread { chain.close() })

    val addr = "0.0.0.0:$port"
    logger.info { "anychain API listening on http://$addr" }
    println("anychain API listening on http://$addr")

    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        install(ContentNegotiation) { json() }
        routing {
            blockRoutes(chain)
        }
    }.start(wait = true)
}
