use axum::{
    extract::{Path, State},
    http::StatusCode,
    response::Json,
};

use crate::models::{AddBlockRequest, BlockDto, SharedChain};

pub async fn list_blocks(State(chain): State<SharedChain>) -> Json<Vec<BlockDto>> {
    let chain = chain.lock().unwrap();
    let blocks = chain.blocks().into_iter().map(BlockDto::from).collect();
    Json(blocks)
}

pub async fn add_block(
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

pub async fn get_block(
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

pub async fn validate_chain(State(chain): State<SharedChain>) -> StatusCode {
    let chain = chain.lock().unwrap();
    if chain.is_valid() {
        StatusCode::OK
    } else {
        StatusCode::CONFLICT
    }
}
