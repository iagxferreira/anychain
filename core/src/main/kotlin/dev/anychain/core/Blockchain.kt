package dev.anychain.core

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import org.rocksdb.Options
import org.rocksdb.RocksDB
import org.rocksdb.RocksDBException
import java.nio.charset.StandardCharsets

private val logger = KotlinLogging.logger {}

private val LAST_KEY = "LAST".toByteArray(StandardCharsets.UTF_8)

/**
 * A chain of [Block]s persisted in an embedded RocksDB database, keyed by block hash. The
 * `"LAST"` key always points to the tip of the chain.
 */
@OptIn(ExperimentalSerializationApi::class)
class Blockchain private constructor(
    private val db: RocksDB,
    private var tipBlock: Block,
) : AutoCloseable {

    /** Hash of the tip (most recently mined) block. */
    val tip: String get() = tipBlock.hash

    /** Mines a new block containing a single transaction with [data] and appends it to the chain. */
    fun addBlock(data: String): Block {
        val tx = Transaction.new(data)
        val block = Block.new(listOf(tx), previousHash = tipBlock.hash, height = tipBlock.height + 1)

        putBlock(db, block)
        db.putOrThrow(LAST_KEY, block.hash.toByteArray(StandardCharsets.UTF_8))
        tipBlock = block

        return block
    }

    /** Returns a block by its hash, or `null` if not found. */
    fun getBlock(hash: String): Block? = fetchBlock(db, hash)

    /** Returns all blocks from tip to genesis. */
    fun blocks(): List<Block> = walkFromTip().toList()

    /** Validates every block in the chain. */
    fun isValid(): Boolean {
        val chain = blocks()
        for ((index, block) in chain.withIndex()) {
            if (!block.isValid()) return false
            if (index + 1 < chain.size && block.previousHash != chain[index + 1].hash) return false
        }
        return true
    }

    /** Number of blocks in the chain (genesis counts as 1). */
    fun height(): Long = tipBlock.height + 1

    override fun close() {
        db.close()
    }

    private fun walkFromTip(): Sequence<Block> = sequence {
        var current: String? = tipBlock.hash
        while (!current.isNullOrEmpty()) {
            val block = fetchBlock(db, current) ?: return@sequence
            yield(block)
            current = block.previousHash
        }
    }

    companion object {
        init {
            RocksDB.loadLibrary()
        }

        /** Opens (or creates) a blockchain at the given filesystem path. */
        fun open(path: String): Blockchain {
            logger.info { "Opening blockchain at '$path'" }
            val options = Options().setCreateIfMissing(true)
            val db = try {
                RocksDB.open(options, path)
            } catch (e: RocksDBException) {
                throw DatabaseException(e)
            }

            val lastHash = db.getOrThrow(LAST_KEY)
            val tipBlock = if (lastHash == null) {
                val genesis = Block.genesis()
                putBlock(db, genesis)
                db.putOrThrow(LAST_KEY, genesis.hash.toByteArray(StandardCharsets.UTF_8))
                logger.info { "Created genesis block: ${genesis.hash}" }
                genesis
            } else {
                val hash = String(lastHash, StandardCharsets.UTF_8)
                checkNotNull(fetchBlock(db, hash)) { "tip block '$hash' missing from database" }
            }

            return Blockchain(db, tipBlock)
        }

        private fun putBlock(db: RocksDB, block: Block) {
            db.putOrThrow(block.hash.toByteArray(StandardCharsets.UTF_8), Cbor.encodeToByteArray(block))
        }

        private fun fetchBlock(db: RocksDB, hash: String): Block? {
            val bytes = db.getOrThrow(hash.toByteArray(StandardCharsets.UTF_8)) ?: return null
            return try {
                Cbor.decodeFromByteArray<Block>(bytes)
            } catch (e: Exception) {
                throw SerializationException(e)
            }
        }

        private fun RocksDB.getOrThrow(key: ByteArray): ByteArray? =
            try {
                get(key)
            } catch (e: RocksDBException) {
                throw DatabaseException(e)
            }

        private fun RocksDB.putOrThrow(key: ByteArray, value: ByteArray) {
            try {
                put(key, value)
            } catch (e: RocksDBException) {
                throw DatabaseException(e)
            }
        }
    }
}
