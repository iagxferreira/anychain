use axum::{routing::get, Router};

use crate::{
    handlers::{add_block, get_block, list_blocks, validate_chain},
    models::SharedChain,
};

pub fn build(state: SharedChain) -> Router {
    Router::new()
        .route("/blocks", get(list_blocks).post(add_block))
        .route("/blocks/:hash", get(get_block))
        .route("/validate", get(validate_chain))
        .with_state(state)
}
