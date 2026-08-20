package dev.anychain.api

import dev.anychain.core.Block
import dev.anychain.core.Blockchain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Serializes concurrent access to a [Blockchain] using a suspending mutex, so that mining a
 * block (CPU-bound proof-of-work) never blocks a server thread the way holding a
 * `java.util.concurrent` lock across the same work would.
 */
class ChainHandle(private val chain: Blockchain) : AutoCloseable {
    private val mutex = Mutex()

    suspend fun blocks(): List<Block> = mutex.withLock { chain.blocks() }

    suspend fun getBlock(hash: String): Block? = mutex.withLock { chain.getBlock(hash) }

    suspend fun isValid(): Boolean = mutex.withLock { chain.isValid() }

    suspend fun addBlock(data: String): Block = mutex.withLock {
        withContext(Dispatchers.Default) { chain.addBlock(data) }
    }

    override fun close() = chain.close()
}
