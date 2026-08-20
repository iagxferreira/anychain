package dev.anychain.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TransactionTest {

    @Test
    fun `new sets data`() {
        val tx = Transaction.new("hello")
        assertEquals("hello", tx.data)
    }

    @Test
    fun `id is 64 hex chars`() {
        val tx = Transaction.new("hello")
        assertEquals(64, tx.id.length)
        assertTrue(tx.id.all { it.isDigit() || it in 'a'..'f' })
    }

    @Test
    fun `different data produces different ids`() {
        val a = Transaction.new("foo")
        val b = Transaction.new("bar")
        assertNotEquals(a.id, b.id)
    }

    @Test
    fun `timestamp is non-zero`() {
        val tx = Transaction.new("x")
        assertTrue(tx.timestamp > 0)
    }
}
