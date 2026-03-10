use serde::{Deserialize, Serialize};

use anychain_core::Block;

pub type SharedChain = std::sync::Arc<std::sync::Mutex<anychain_core::Blockchain>>;

#[derive(Deserialize)]
pub struct AddBlockRequest {
    pub data: String,
}

#[derive(Serialize)]
pub struct TransactionDto {
    pub id: String,
    pub data: String,
    pub timestamp: u128,
}

#[derive(Serialize)]
pub struct BlockDto {
    pub hash: String,
    pub previous_hash: String,
    pub height: u64,
    pub timestamp: u128,
    pub nonce: u64,
    pub transactions: Vec<TransactionDto>,
}

impl From<Block> for BlockDto {
    fn from(b: Block) -> Self {
        BlockDto {
            hash: b.hash().to_string(),
            previous_hash: b.previous_hash,
            height: b.height,
            timestamp: b.timestamp,
            nonce: b.nonce,
            transactions: b
                .transactions
                .into_iter()
                .map(|tx| TransactionDto { id: tx.id, data: tx.data, timestamp: tx.timestamp })
                .collect(),
        }
    }
}
