use serde::{Deserialize, Serialize};
use sha2::{Digest, Sha256};
use std::time::SystemTime;

use crate::error::Result;
use crate::transaction::Transaction;

/// Number of leading zero hex characters required for a valid hash (proof-of-work difficulty).
const DIFFICULTY: usize = 4;

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct Block {
    pub height: u64,
    pub timestamp: u128,
    pub transactions: Vec<Transaction>,
    pub previous_hash: String,
    pub hash: String,
    pub nonce: u64,
}

impl Block {
    /// Creates the genesis block (first block in the chain).
    pub fn genesis() -> Result<Block> {
        Block::new(vec![Transaction::new("Genesis Block")], String::new(), 0)
    }

    /// Mines a new block with the given transactions on top of `previous_hash`.
    pub fn new(transactions: Vec<Transaction>, previous_hash: String, height: u64) -> Result<Block> {
        let timestamp = SystemTime::now()
            .duration_since(SystemTime::UNIX_EPOCH)?
            .as_millis();

        let mut block = Block {
            height,
            timestamp,
            transactions,
            previous_hash,
            hash: String::new(),
            nonce: 0,
        };

        block.mine();
        Ok(block)
    }

    pub fn hash(&self) -> &str {
        &self.hash
    }

    /// Verifies that the stored hash satisfies the difficulty target.
    pub fn is_valid(&self) -> bool {
        let hash = self.compute_hash(self.nonce);
        hash == self.hash && hash.starts_with(&"0".repeat(DIFFICULTY))
    }

    fn compute_hash(&self, nonce: u64) -> String {
        let mut hasher = Sha256::new();
        hasher.update(self.previous_hash.as_bytes());
        hasher.update(self.timestamp.to_le_bytes());
        hasher.update(self.height.to_le_bytes());
        hasher.update(nonce.to_le_bytes());
        for tx in &self.transactions {
            hasher.update(tx.id.as_bytes());
            hasher.update(tx.data.as_bytes());
        }
        format!("{:x}", hasher.finalize())
    }

    fn mine(&mut self) {
        let target = "0".repeat(DIFFICULTY);
        log::info!("Mining block at height {}...", self.height);
        loop {
            let hash = self.compute_hash(self.nonce);
            if hash.starts_with(&target) {
                self.hash = hash;
                log::info!("Block mined: {} (nonce={})", self.hash, self.nonce);
                return;
            }
            self.nonce += 1;
        }
    }
}
