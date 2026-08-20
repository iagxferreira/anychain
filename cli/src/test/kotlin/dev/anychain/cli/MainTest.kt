package dev.anychain.cli

import com.github.ajalt.clikt.testing.test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class MainTest {

    @TempDir
    lateinit var tempDir: Path

    private fun dbArgs(vararg args: String): List<String> =
        listOf("--db", tempDir.resolve("db").toString()) + args

    @Test
    fun `add mines and reports a block`() {
        val result = Anychain().test(dbArgs("add", "hello"))
        assertEquals(0, result.statusCode)
        assertContains(result.output, "Mining block...")
        assertContains(result.output, "Block added!")
        assertContains(result.output, "Height : 1")
    }

    @Test
    fun `print on a fresh chain shows the genesis block`() {
        val result = Anychain().test(dbArgs("print"))
        assertEquals(0, result.statusCode)
        assertContains(result.output, "Height      : 0")
        assertContains(result.output, "Genesis Block")
    }

    @Test
    fun `print after adding shows both blocks`() {
        Anychain().test(dbArgs("add", "first"))
        val result = Anychain().test(dbArgs("print"))
        assertEquals(0, result.statusCode)
        assertContains(result.output, "Height      : 0")
        assertContains(result.output, "Height      : 1")
        assertContains(result.output, "first")
    }

    @Test
    fun `validate on a fresh chain succeeds`() {
        val result = Anychain().test(dbArgs("validate"))
        assertEquals(0, result.statusCode)
        assertContains(result.output, "Chain is valid.")
    }
}
