use std::sync::{Arc, Mutex};

use axum::{
    extract::{Path, State},
    http::StatusCode,
    response::Json,
    routing::get,
    Router,
};
use serde::{Deserialize, Serialize};

use anychain_core::{Block, Blockchain};

type SharedChain = Arc<Mutex<Blockchain>>;

// ── Request / Response types ──────────────────────────────────────────────────

#[derive(Deserialize)]
struct AddBlockRequest {
    data: String,
}

#[derive(Serialize)]
struct TransactionDto {
    id: String,
    data: String,
    timestamp: u128,
}

#[derive(Serialize)]
struct BlockDto {
    hash: String,
    previous_hash: String,
    height: u64,
    timestamp: u128,
    nonce: u64,
    transactions: Vec<TransactionDto>,
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

// ── Handlers ──────────────────────────────────────────────────────────────────

async fn list_blocks(State(chain): State<SharedChain>) -> Json<Vec<BlockDto>> {
    let chain = chain.lock().unwrap();
    let blocks = chain.blocks().into_iter().map(BlockDto::from).collect();
    Json(blocks)
}

async fn add_block(
    State(chain): State<SharedChain>,
    Json(req): Json<AddBlockRequest>,
) -> Result<Json<BlockDto>, StatusCode> {
    let mut chain = chain.lock().unwrap();
    chain
        .add_block(req.data)
        .map(|b| Json(BlockDto::from(b)))
        .map_err(|e| {
            log::error!("Failed to add block: {e}");
            StatusCode::INTERNAL_SERVER_ERROR
        })
}

async fn get_block(
    State(chain): State<SharedChain>,
    Path(hash): Path<String>,
) -> Result<Json<BlockDto>, StatusCode> {
    let chain = chain.lock().unwrap();
    match chain.get_block(&hash) {
        Ok(Some(block)) => Ok(Json(BlockDto::from(block))),
        Ok(None) => Err(StatusCode::NOT_FOUND),
        Err(e) => {
            log::error!("Failed to get block: {e}");
            Err(StatusCode::INTERNAL_SERVER_ERROR)
        }
    }
}

async fn validate_chain(State(chain): State<SharedChain>) -> StatusCode {
    let chain = chain.lock().unwrap();
    if chain.is_valid() {
        StatusCode::OK
    } else {
        StatusCode::CONFLICT
    }
}

// ── Entry point ───────────────────────────────────────────────────────────────

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    env_logger::init();

    let db_path = std::env::var("ANYCHAIN_DB").unwrap_or_else(|_| "/tmp/anychain".to_string());
    let port = std::env::var("PORT").unwrap_or_else(|_| "3000".to_string());

    let bc = Blockchain::open(&db_path)?;
    let state: SharedChain = Arc::new(Mutex::new(bc));

    let app = Router::new()
        .route("/blocks", get(list_blocks).post(add_block))
        .route("/blocks/:hash", get(get_block))
        .route("/validate", get(validate_chain))
        .with_state(state);

    let addr = format!("0.0.0.0:{port}");
    log::info!("anychain API listening on http://{addr}");
    println!("anychain API listening on http://{addr}");

    let listener = tokio::net::TcpListener::bind(&addr).await?;
    axum::serve(listener, app).await?;

    Ok(())
}
