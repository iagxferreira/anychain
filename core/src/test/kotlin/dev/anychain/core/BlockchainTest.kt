package dev.anychain.core

import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BlockchainTest {

    @TempDir
    lateinit var tempDir: Path

    private fun openChain(): Blockchain = Blockchain.open(tempDir.resolve("db").toString())

    @Test
    fun `opens with genesis block`() {
        openChain().use { chain ->
            assertEquals(1, chain.height())
        }
    }

    @Test
    fun `add block increases height`() {
        openChain().use { chain ->
            chain.addBlock("tx1")
            assertEquals(2, chain.height())
        }
    }

    @Test
    fun `blocks returns all in order`() {
        openChain().use { chain ->
            chain.addBlock("a")
            chain.addBlock("b")
            val blocks = chain.blocks()
            assertEquals(3, blocks.size)
            // blocks() goes tip -> genesis, so heights descend
            assertTrue(blocks[0].height > blocks[1].height)
        }
    }

    @Test
    fun `get block by hash`() {
        openChain().use { chain ->
            val added = chain.addBlock("hello")
            val fetched = chain.getBlock(added.hash)
            assertEquals(added.hash, fetched?.hash)
        }
    }

    @Test
    fun `get block missing returns null`() {
        openChain().use { chain ->
            assertNull(chain.getBlock("nonexistent"))
        }
    }

    @Test
    fun `fresh chain is valid`() {
        openChain().use { chain ->
            assertTrue(chain.isValid())
        }
    }

    @Test
    fun `chain with blocks is valid`() {
        openChain().use { chain ->
            chain.addBlock("x")
            chain.addBlock("y")
            assertTrue(chain.isValid())
        }
    }

    @Test
    fun `tip matches last added block`() {
        openChain().use { chain ->
            val block = chain.addBlock("tip-test")
            assertEquals(block.hash, chain.tip)
        }
    }
}
