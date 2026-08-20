package dev.anychain.core

import java.security.MessageDigest
import java.time.Clock

@kotlinx.serialization.Serializable
data class Transaction(
    val id: String,
    val data: String,
    val timestamp: Long,
) {
    companion object {
        fun new(data: String, clock: Clock = Clock.systemUTC()): Transaction {
            val timestamp = clock.millis()

            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(data.toByteArray(Charsets.UTF_8))
            digest.update(timestamp.toLittleEndianBytes())
            val id = digest.digest().toHexString()

            return Transaction(id = id, data = data, timestamp = timestamp)
        }
    }
}
