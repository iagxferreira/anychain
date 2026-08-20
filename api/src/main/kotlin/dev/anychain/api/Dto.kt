package dev.anychain.api

import dev.anychain.core.Block
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddBlockRequest(val data: String)

@Serializable
data class TransactionDto(
    val id: String,
    val data: String,
    val timestamp: Long,
)

@Serializable
data class BlockDto(
    val hash: String,
    @SerialName("previous_hash") val previousHash: String,
    val height: Long,
    val timestamp: Long,
    val nonce: Long,
    val transactions: List<TransactionDto>,
)

fun Block.toDto(): BlockDto = BlockDto(
    hash = hash,
    previousHash = previousHash,
    height = height,
    timestamp = timestamp,
    nonce = nonce,
    transactions = transactions.map { TransactionDto(it.id, it.data, it.timestamp) },
)
