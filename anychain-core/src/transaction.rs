use serde::{Deserialize, Serialize};
use sha2::{Digest, Sha256};
use std::time::SystemTime;

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct Transaction {
    pub id: String,
    pub data: String,
    pub timestamp: u128,
}

impl Transaction {
    pub fn new(data: impl Into<String>) -> Self {
        let data = data.into();
        let timestamp = SystemTime::now()
            .duration_since(SystemTime::UNIX_EPOCH)
            .unwrap_or_default()
            .as_millis();

        let mut hasher = Sha256::new();
        hasher.update(data.as_bytes());
        hasher.update(timestamp.to_le_bytes());
        let id = format!("{:x}", hasher.finalize());

        Transaction { id, data, timestamp }
    }
}
