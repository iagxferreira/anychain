use thiserror::Error;

#[derive(Debug, Error)]
pub enum Error {
    #[error("database error: {0}")]
    Database(#[from] sled::Error),

    #[error("serialization error: {0}")]
    Serialization(#[from] bincode::Error),

    #[error("UTF-8 conversion error: {0}")]
    Utf8(#[from] std::string::FromUtf8Error),

    #[error("system time error: {0}")]
    SystemTime(#[from] std::time::SystemTimeError),

    #[error("block not found: {0}")]
    BlockNotFound(String),

    #[error("blockchain is empty")]
    EmptyChain,
}

pub type Result<T> = std::result::Result<T, Error>;
