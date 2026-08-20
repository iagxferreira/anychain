package dev.anychain.api

import dev.anychain.core.Blockchain
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoutesTest {

    @TempDir
    lateinit var tempDir: Path

    private fun testApp(block: suspend io.ktor.client.HttpClient.() -> Unit) = testApplication {
        val chain = ChainHandle(Blockchain.open(tempDir.resolve("db").toString()))
        application {
            install(ContentNegotiation) { json() }
            routing { blockRoutes(chain) }
        }
        client.block()
    }

    @Test
    fun `list blocks returns the genesis block`() = testApp {
        val response = get("/blocks")
        assertEquals(HttpStatusCode.OK, response.status)
        val blocks = Json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(1, blocks.size)
        assertEquals("", blocks[0].jsonObject["previous_hash"]!!.jsonPrimitive.content)
    }

    @Test
    fun `post blocks mines and returns a block`() = testApp {
        val response = post("/blocks") {
            contentType(ContentType.Application.Json)
            setBody("""{"data":"hello"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals(1L, body["height"]!!.jsonPrimitive.long)
        assertTrue(body["hash"]!!.jsonPrimitive.content.isNotEmpty())
        val txs = body["transactions"]!!.jsonArray
        assertEquals("hello", txs[0].jsonObject["data"]!!.jsonPrimitive.content)
    }

    @Test
    fun `get block by hash returns the block`() = testApp {
        val added = post("/blocks") {
            contentType(ContentType.Application.Json)
            setBody("""{"data":"hello"}""")
        }
        val hash = Json.parseToJsonElement(added.bodyAsText()).jsonObject["hash"]!!.jsonPrimitive.content

        val response = get("/blocks/$hash")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `get block missing returns 404`() = testApp {
        val response = get("/blocks/nonexistent")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `validate on a fresh chain returns 200`() = testApp {
        val response = get("/validate")
        assertEquals(HttpStatusCode.OK, response.status)
    }
}
