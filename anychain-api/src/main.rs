mod handlers;
mod models;
mod router;

use std::sync::{Arc, Mutex};

use anychain_core::Blockchain;

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    env_logger::init();

    let db_path = std::env::var("ANYCHAIN_DB").unwrap_or_else(|_| "/tmp/anychain".to_string());
    let port = std::env::var("PORT").unwrap_or_else(|_| "3000".to_string());

    let bc = Blockchain::open(&db_path)?;
    let state = Arc::new(Mutex::new(bc));

    let app = router::build(state);

    let addr = format!("0.0.0.0:{port}");
    log::info!("anychain API listening on http://{addr}");
    println!("anychain API listening on http://{addr}");

    let listener = tokio::net::TcpListener::bind(&addr).await?;
    axum::serve(listener, app).await?;

    Ok(())
}
