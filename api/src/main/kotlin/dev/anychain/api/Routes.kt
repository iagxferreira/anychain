package dev.anychain.api

import dev.anychain.core.AnychainException
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post

private val logger = KotlinLogging.logger {}

fun Routing.blockRoutes(chain: ChainHandle) {
    get("/blocks") {
        call.respond(chain.blocks().map { it.toDto() })
    }

    post("/blocks") {
        val request = call.receive<AddBlockRequest>()
        try {
            call.respond(chain.addBlock(request.data).toDto())
        } catch (e: AnychainException) {
            logger.error(e) { "Failed to add block" }
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    get("/blocks/{hash}") {
        val hash = call.parameters["hash"]!!
        try {
            val block = chain.getBlock(hash)
            if (block == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                call.respond(block.toDto())
            }
        } catch (e: AnychainException) {
            logger.error(e) { "Failed to get block" }
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    get("/validate") {
        val status = if (chain.isValid()) HttpStatusCode.OK else HttpStatusCode.Conflict
        call.respond(status)
    }
}
