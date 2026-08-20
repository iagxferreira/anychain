package dev.anychain.core

import io.github.oshai.kotlinlogging.KotlinLogging
import java.security.MessageDigest
import java.time.Clock

private val logger = KotlinLogging.logger {}

/** Number of leading zero hex characters required for a valid hash (proof-of-work difficulty). */
private const val DIFFICULTY = 4

@kotlinx.serialization.Serializable
data class Block(
    val height: Long,
    val timestamp: Long,
    val transactions: List<Transaction>,
    val previousHash: String,
    val hash: String,
    val nonce: Long,
) {
    /** Verifies that the stored hash satisfies the difficulty target. */
    fun isValid(): Boolean {
        val computed = computeHash(nonce)
        return computed == hash && hash.startsWith(TARGET)
    }

    private fun computeHash(nonce: Long): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(previousHash.toByteArray(Charsets.UTF_8))
        digest.update(timestamp.toLittleEndianBytes())
        digest.update(height.toLittleEndianBytes())
        digest.update(nonce.toLittleEndianBytes())
        for (tx in transactions) {
            digest.update(tx.id.toByteArray(Charsets.UTF_8))
            digest.update(tx.data.toByteArray(Charsets.UTF_8))
        }
        return digest.digest().toHexString()
    }

    companion object {
        private val TARGET = "0".repeat(DIFFICULTY)

        /** Creates the genesis block (first block in the chain). */
        fun genesis(clock: Clock = Clock.systemUTC()): Block =
            new(listOf(Transaction.new("Genesis Block", clock)), previousHash = "", height = 0, clock = clock)

        /** Mines a new block with the given transactions on top of [previousHash]. */
        fun new(
            transactions: List<Transaction>,
            previousHash: String,
            height: Long,
            clock: Clock = Clock.systemUTC(),
        ): Block {
            val block = Block(
                height = height,
                timestamp = clock.millis(),
                transactions = transactions,
                previousHash = previousHash,
                hash = "",
                nonce = 0,
            )
            return block.mine()
        }
    }

    private fun mine(): Block {
        logger.info { "Mining block at height $height..." }
        var candidateNonce = 0L
        while (true) {
            val candidateHash = computeHash(candidateNonce)
            if (candidateHash.startsWith(TARGET)) {
                val mined = copy(nonce = candidateNonce, hash = candidateHash)
                logger.info { "Block mined: $candidateHash (nonce=$candidateNonce)" }
                return mined
            }
            candidateNonce++
        }
    }
}
