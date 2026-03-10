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

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn new_sets_data() {
        let tx = Transaction::new("hello");
        assert_eq!(tx.data, "hello");
    }

    #[test]
    fn id_is_64_hex_chars() {
        let tx = Transaction::new("hello");
        assert_eq!(tx.id.len(), 64);
        assert!(tx.id.chars().all(|c| c.is_ascii_hexdigit()));
    }

    #[test]
    fn different_data_produces_different_ids() {
        let a = Transaction::new("foo");
        let b = Transaction::new("bar");
        assert_ne!(a.id, b.id);
    }

    #[test]
    fn timestamp_is_non_zero() {
        let tx = Transaction::new("x");
        assert!(tx.timestamp > 0);
    }
}
