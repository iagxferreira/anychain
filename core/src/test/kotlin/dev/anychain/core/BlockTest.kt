package dev.anychain.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val DIFFICULTY = 4

class BlockTest {

    @Test
    fun `genesis is valid`() {
        val block = Block.genesis()
        assertTrue(block.isValid())
    }

    @Test
    fun `genesis height is zero`() {
        val block = Block.genesis()
        assertEquals(0, block.height)
    }

    @Test
    fun `genesis previous hash is empty`() {
        val block = Block.genesis()
        assertTrue(block.previousHash.isEmpty())
    }

    @Test
    fun `hash meets difficulty`() {
        val block = Block.genesis()
        assertTrue(block.hash.startsWith("0".repeat(DIFFICULTY)))
    }

    @Test
    fun `new block links to previous`() {
        val genesis = Block.genesis()
        val txs = listOf(Transaction.new("next"))
        val block = Block.new(txs, genesis.hash, 1)
        assertEquals(genesis.hash, block.previousHash)
        assertTrue(block.isValid())
    }

    @Test
    fun `tampered block is invalid`() {
        val block = Block.genesis().copy(hash = "0".repeat(68))
        assertFalse(block.isValid())
    }
}
